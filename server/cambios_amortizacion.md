# Cambios: Amortización de Cuotas (Compra / Venta)

**Fecha:** 2026-08-04

## 1. Resumen

Se agregó soporte para amortizar cuotas de **venta** (antes solo existía para compra) y se corrigió un error contable por el cual la amortización de una **compra** se registraba como **INGRESO** en vez de **EGRESO**.

---

## 2. Problema detectado

Al amortizar una cuota de una **compra** (pago a un proveedor), el sistema generaba el comprobante contable como si fuera un **INGRESO** de dinero, cuando en realidad es una **salida de dinero (EGRESO)**.

Esto se debía a que el código que genera el asiento contable tenía el tipo fijo (`ingreso`), sin importar si la operación era de compra o de venta.

 

### b) Nueva funcionalidad: amortizar cuota de venta
**Repositorio:** `caja` (servidor de caja)
**Archivo:** `src/Component/CajaDetalle.java`

Se agregó el caso `amortizarCuotaVenta`, análogo al ya existente `amortizarCuotaCompra`, para permitir registrar en caja el pago de cuotas de una venta (cobro a un cliente).

---

## 4. Archivos modificados

- `compra_venta/server/src/Component/CompraVenta_Components/AmortizarCuotas.java`
- `caja/server/src/Component/CajaDetalle.java`

---

## 5. Resultado esperado

- Al amortizar una **compra**: el movimiento se refleja como **EGRESO** en el listado contable.
- Al amortizar una **venta**: el movimiento se refleja como **INGRESO**, como corresponde.
- Ya es posible registrar amortizaciones de cuotas de venta desde el módulo de caja.
