# Funcionalidad: Anular Amortización

**Fecha:** 2026-08-04

## 1. Resumen

Se implementó la posibilidad de **anular (revertir) el pago de una cuota** ya amortizada, tanto de una compra como de una venta. Es el equivalente a "deshacer" un pago de cuota si se registró por error o hay que corregirlo.

---

## 2. Problema que resuelve

Antes solo se podía anular una **compra o venta completa** (`anularVenta` / `anularCompra`). No existía forma de anular **un solo pago de cuota** sin afectar el resto de la operación (por ejemplo, si un cliente pagó 2 de 5 cuotas y una de esas 2 se registró mal, no había forma de revertir solo esa).

---

## 3. Regla de negocio principal

- Solo se puede anular la **amortización más reciente** de ese cliente/proveedor. Esto evita que se descuadren los saldos si hay pagos posteriores encima.
- Si el pago se hizo con varios medios de pago a la vez (ej. parte efectivo, parte transferencia), se anulan **todas las partes juntas**, como una sola unidad.
- Los pagos de cuotas hechos **antes de esta actualización** no se pueden anular automáticamente (el sistema lo detecta y avisa).

---

## 4. Qué pasa al anular una amortización

1. Se busca el pago original y se valida que sea el más reciente de ese cliente/proveedor.
2. Se genera un **asiento contable de reversión** (tipo traspaso), con los montos exactamente invertidos.
3. Se marca la cuota como **no pagada** nuevamente (se recalcula el saldo pendiente).
4. En caja se registra un movimiento de reversión (`reversion_amortizacion`) por el monto contrario, en la caja abierta del punto de venta correspondiente.

---

## 5. Archivos involucrados

**Repositorio `compra_venta`**
- `server/src/Component/CompraVenta_Components/AnularAmortizacion.java` *(nuevo)*
- `server/src/Component/CompraVenta.java` — nuevo caso `anularAmortizacion`
- `server/src/Component/CompraVenta_Components/AmortizarCuotas.java` — ahora guarda `key_caja_detalle` en cada amortización para poder ubicarla al anularla

**Repositorio `caja`**
- `src/Component/CajaDetalle.java` — nuevo caso `anularAmortizacion` y método `anularAmortizacion(...)`, que coordina la reversión contable con el movimiento de caja

---

## 6. Estado

⚠️ Estos cambios están escritos y agregados (`git add`) en ambos repositorios, pero **aún no se han commiteado ni desplegado**. Falta probar el flujo completo antes de pasar a producción.
