# Anulación de venta/compra — flujo y cambios

Documenta cómo funciona el botón "Anular venta"/"Anular compra", qué se
modificó y qué bugs quedaron detectados (sin corregir) en el servicio
`compra_venta`.

## 1. Flujo completo

```
Frontend (app)
  Pages/caja/reporteMoviminetos.js  → botón "Anular venta"/"Anular compra"
  MDL/caja/index.ts → anular_venta() / anular_compra()
        service: "caja", component: "caja_detalle",
        type: "anularVenta" | "anularCompra"
        { key_compra_venta, key_caja: MDL.caja.activa?.key, key_empresa, key_usuario }
        │
        ▼
Backend "caja"
  Component/CajaDetalle.java → onMessage() → anular(obj, session, tipo)
        │  (ver sección 2: cálculo de caja destino)
        │  obj.put("component", "compra_venta")
        ▼  SocketCliente.sendSinc("compra_venta", obj)
Backend "compra_venta"
  Component/CompraVenta.java → onMessage()
        case "anularVenta" → new Anular(obj, session, "venta")
        case "anularCompra" → new Anular(obj, session, "compra")
  Component/CompraVenta_Components/Anular.java
        - marca compra_venta.estado = 0
        - revierte cuotas / cuota_amortizacion pendientes (asiento contable)
        - revierte descuentos aplicados
        - avisa a "inventario" (SocketCliente.sendSinc)
        - responde { estado: "exito" | "error" }
        │
        ▼ (si estado == "exito")
Backend "caja" (continúa en CajaDetalle.anular)
  Inserta en caja_detalle una fila espejo por cada movimiento original,
  con monto negado y tipo "anulacion_venta"/"anulacion_compra".
```

## 2. De dónde sale `key_caja` / `key_punto_venta` en la reversión

Antes del cambio, `CajaDetalle.anular` usaba el `key_caja` que manda el
frontend (`MDL.caja.activa?.key`, la caja **activa de quien anula**) solo
para resolver `key_punto_venta`, y las filas de reversión en
`caja_detalle` heredaban el `key_caja` **original de la venta** (porque
se reutilizaban los mismos objetos devueltos por
`getByKeyCompraVenta`, sin tocar ese campo).

Esto significaba que anular una venta podía intentar insertar un
movimiento en una caja ya **cerrada** (la de la venta original), sin
relación con la caja que el cajero tiene abierta hoy.

### Cambio aplicado

Archivo: [`src/Component/CajaDetalle.java`](../src/Component/CajaDetalle.java) — método `anular(...)`.

1. Se obtiene el punto de venta a partir de la **caja original de la venta**
   (no de la caja activa del usuario que anula):
   ```java
   String key_caja_original = detalles.getJSONObject(JSONObject.getNames(detalles)[0]).getString("key_caja");
   JSONObject cajaOriginal = Caja.getByKey(key_caja_original);
   String key_punto_venta = cajaOriginal.optString("key_punto_venta");
   ```
2. Se busca la **caja actualmente abierta** (`estado == 1`) para ese punto
   de venta con `Caja.getByKeyPuntoVenta(key_punto_venta)` (función SQL
   `get_abiertas_punto_venta`). Si no hay ninguna abierta, se lanza error
   y no se permite anular:
   ```java
   JSONObject cajasPuntoVenta = Caja.getByKeyPuntoVenta(key_punto_venta);
   JSONObject cajaDestino = null;
   for (String k : JSONObject.getNames(cajasPuntoVenta)) {
       JSONObject c = cajasPuntoVenta.getJSONObject(k);
       if (c.optInt("estado", 0) == 1) { cajaDestino = c; break; }
   }
   if (cajaDestino == null) {
       throw new Exception("No hay una caja abierta para el punto de venta de la venta original");
   }
   ```
3. Cada fila de reversión que se inserta en `caja_detalle` ahora fuerza
   `key_caja = key_caja_destino` (la caja abierta encontrada), en vez de
   heredar el `key_caja` de la fila original.

### Efecto

- La anulación **ya no puede caer en una caja cerrada**.
- Si el punto de venta de la venta original no tiene ninguna caja abierta
  en este momento, la anulación falla con un mensaje claro en vez de
  fallar más adelante (NPE) o insertar datos en un cierre ya hecho.
- El `key_caja` que manda el frontend (`MDL.caja.activa?.key`) queda sin
  uso en este flujo; no hace falta tocar el frontend para que funcione.

## 3. Bugs detectados en `Anular.java` (compra_venta) — pendientes de corregir

Archivo: [`Component/CompraVenta_Components/Anular.java`](../../compra_venta/compra_venta/server/src/Component/CompraVenta_Components/Anular.java)
(repo `compra_venta`).

| # | Línea | Problema | Impacto |
|---|-------|----------|---------|
| 1 | `Anular.java:105` | `tipo_cambio = monto_base / monto_base` (se divide entre sí mismo, siempre da `1`). El patrón correcto (usado en `cuota_amortizacion`, línea 140) es `monto / monto_base`. | El monto en moneda extranjera (`monto_me`) de la cuota pendiente revertida nunca se calcula. |
| 2 | `Anular.java:87-99` | La consulta trae la *definición* del descuento (`descuento.*`), no el monto individual aplicado (`compra_venta_descuento.monto`). El código usa `compraVenta.getDouble("descuento")` (el total agregado) **dentro del for** de cada descuento. | Si una venta/compra tiene más de un descuento aplicado, se genera un asiento de reversión duplicado por cada descuento, cada uno con el monto **total**, sobre-revirtiendo. |
| 2b | `Anular.java:92-97` | La reversión de descuento siempre usa `"haber"`, sin distinguir `tipo` (venta/compra) como sí hace la reversión de cuotas. | En una anulación de **compra** con descuento, el asiento podría quedar con la dirección contable invertida. |
| 3 | `Anular.java:110-113` | `tipoMovimiento` está invertido: para `venta` queda `"pagar"` y para `compra` queda `"cobrar"` (debería ser al revés). | Solo afecta el texto de la glosa del asiento contable, no el monto ni la dirección debe/haber. |

Ninguno de estos bugs se corrigió todavía — quedan documentados para una
próxima iteración.
