# `CajaDetalle.anular(...)` — antes y después

Archivo: [`src/Component/CajaDetalle.java`](../src/Component/CajaDetalle.java)

## Antes

Usaba el `key_caja` que manda el frontend (la caja **activa** de quien
anula) solo para resolver `key_punto_venta`. Las filas de reversión en
`caja_detalle` heredaban el `key_caja` **original de la venta** (nunca
se sobrescribía), por lo que la reversión podía caer en una caja ya
cerrada.

```java
public static void anular(JSONObject obj, SSSessionAbstract session, String tipo) {
    ConectInstance conectInstance = null;

    try {
        conectInstance = new ConectInstance();
        conectInstance.Transacction();

        String key_compra_venta = obj.optString("key_compra_venta");

        JSONObject detalles = CajaDetalle.getByKeyCompraVenta(key_compra_venta);

        JSONObject caja = Caja.getByKey(obj.getString("key_caja"));

        if (detalles == null) {
            throw new Exception("No se encontraron detalles para la compra venta: " + key_compra_venta);
        }

        obj.put("caja_detalle", detalles);
        obj.put("key_punto_venta", caja.optString("key_punto_venta"));

        JSONObject empresaTipoPago = EmpresaTipoPago.getAll(obj.getString("key_empresa"));
        obj.put("component", "compra_venta");
        obj.put("empresa_tipo_pago", empresaTipoPago);

        // System.out.println("Anulando " + tipo + " con data: " + obj);
        JSONObject data = SocketCliente.sendSinc("compra_venta", obj);
        System.out.println("Respuesta de anulación de " + tipo + ": " + data);

        if (data.getString("estado").equals("error")) {
            throw new Exception(data.optString("error", "Error al anular la " + tipo + " en compra venta"));
        }

        System.out.println("printtt");
        System.out.println("");

        if (detalles != null && !detalles.isEmpty()) {
            for (int i = 0; i < JSONObject.getNames(detalles).length; i++) {
                String key = JSONObject.getNames(detalles)[i];
                JSONObject detalle = detalles.getJSONObject(key);

                detalle.put("monto", detalle.getDouble("monto") * -1);
                detalle.put("descripcion", "ANULACION: " + detalle.optString("descripcion"));
                detalle.put("tipo", "anulacion_" + tipo);
                detalle.put("key_compra_venta", key_compra_venta);
                detalle.put("fecha_on", SUtil.now());
                detalle.put("key", SUtil.uuid());
                conectInstance.insertArray(COMPONENT, new JSONArray().put(detalle));
            }
        }

        obj.put("estado", "exito");
        // System.out.println("Anulación de " + tipo + " exitosa: " + key_compra_venta);
        conectInstance.commit();
    } catch (Exception e) {
        e.printStackTrace();
        // System.out.println("Error al anular la " + tipo + ": " + e.getMessage());
        obj.put("estado", "error");
        obj.put("error", e.getMessage());
        conectInstance.rollback();
    } finally {
        if (conectInstance != null) {
            conectInstance.close();
        }
    }
}
```

## Después

Ahora el punto de venta se calcula a partir de la caja **original de la
venta** (no de la caja activa del usuario), y la reversión se registra
en la caja que esté **actualmente abierta** (`estado == 1`) para ese
punto de venta. Si no hay ninguna caja abierta, se aborta con un error
claro en vez de anular igual o fallar más adelante.

```java
public static void anular(JSONObject obj, SSSessionAbstract session, String tipo) {
    ConectInstance conectInstance = null;

    try {
        conectInstance = new ConectInstance();
        conectInstance.Transacction();

        String key_compra_venta = obj.optString("key_compra_venta");

        JSONObject detalles = CajaDetalle.getByKeyCompraVenta(key_compra_venta);

        if (detalles == null || detalles.isEmpty()) {
            throw new Exception("No se encontraron detalles para la compra venta: " + key_compra_venta);
        }

        // El punto de venta se obtiene de la caja donde se registró la venta original,
        // no de la caja activa de quien anula.
        String key_caja_original = detalles.getJSONObject(JSONObject.getNames(detalles)[0]).getString("key_caja");
        JSONObject cajaOriginal = Caja.getByKey(key_caja_original);
        String key_punto_venta = cajaOriginal.optString("key_punto_venta");

        // La reversión se registra en la caja actualmente ABIERTA de ese punto de venta,
        // no necesariamente en la misma caja (ya cerrada) donde se hizo la venta.
        JSONObject cajasPuntoVenta = Caja.getByKeyPuntoVenta(key_punto_venta);
        JSONObject cajaDestino = null;
        if (cajasPuntoVenta != null && !cajasPuntoVenta.isEmpty()) {
            for (String k : JSONObject.getNames(cajasPuntoVenta)) {
                JSONObject c = cajasPuntoVenta.getJSONObject(k);
                if (c.optInt("estado", 0) == 1) {
                    cajaDestino = c;
                    break;
                }
            }
        }
        if (cajaDestino == null) {
            throw new Exception("No hay una caja abierta para el punto de venta de la venta original");
        }
        String key_caja_destino = cajaDestino.getString("key");

        obj.put("caja_detalle", detalles);
        obj.put("key_punto_venta", key_punto_venta);

        JSONObject empresaTipoPago = EmpresaTipoPago.getAll(obj.getString("key_empresa"));
        obj.put("component", "compra_venta");
        obj.put("empresa_tipo_pago", empresaTipoPago);

        // System.out.println("Anulando " + tipo + " con data: " + obj);
        JSONObject data = SocketCliente.sendSinc("compra_venta", obj);
        System.out.println("Respuesta de anulación de " + tipo + ": " + data);

        if (data.getString("estado").equals("error")) {
            throw new Exception(data.optString("error", "Error al anular la " + tipo + " en compra venta"));
        }

        System.out.println("printtt");
        System.out.println("");

        if (detalles != null && !detalles.isEmpty()) {
            for (int i = 0; i < JSONObject.getNames(detalles).length; i++) {
                String key = JSONObject.getNames(detalles)[i];
                JSONObject detalle = detalles.getJSONObject(key);

                detalle.put("key_caja", key_caja_destino);
                detalle.put("monto", detalle.getDouble("monto") * -1);
                detalle.put("descripcion", "ANULACION: " + detalle.optString("descripcion"));
                detalle.put("tipo", "anulacion_" + tipo);
                detalle.put("key_compra_venta", key_compra_venta);
                detalle.put("fecha_on", SUtil.now());
                detalle.put("key", SUtil.uuid());
                conectInstance.insertArray(COMPONENT, new JSONArray().put(detalle));
            }
        }

        obj.put("estado", "exito");
        // System.out.println("Anulación de " + tipo + " exitosa: " + key_compra_venta);
        conectInstance.commit();
    } catch (Exception e) {
        e.printStackTrace();
        // System.out.println("Error al anular la " + tipo + ": " + e.getMessage());
        obj.put("estado", "error");
        obj.put("error", e.getMessage());
        conectInstance.rollback();
    } finally {
        if (conectInstance != null) {
            conectInstance.close();
        }
    }
}
```

## Resumen de la diferencia

| Aspecto | Antes | Después |
|---|---|---|
| Origen del `key_punto_venta` | `Caja.getByKey(obj.getString("key_caja"))` → caja **activa** del usuario que anula | `Caja.getByKey(key_caja_original)` → caja donde ocurrió **la venta original** |
| Caja destino de la reversión | La caja original de la venta (heredada, sin querer, del `detalle`) | La caja **abierta** (`estado == 1`) de ese punto de venta, buscada explícitamente |
| Validación de `detalles` vacío | Solo chequeaba `null` | Chequea `null` y vacío |
| Sin caja abierta disponible | No se validaba (podía fallar más abajo o insertar en caja cerrada) | Lanza `Exception("No hay una caja abierta para el punto de venta de la venta original")` |
| Campo `key_caja` en cada `detalle` insertado | No se tocaba (quedaba el original) | Se fuerza a `key_caja_destino` |
