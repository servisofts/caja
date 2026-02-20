package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Contabilidad.Contabilidad;
import Servisofts.SPG;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class CajaDetalle {
    public static final String COMPONENT = "caja_detalle";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getByKey":
                getByKey(obj, session);
                break;
            case "registro":
                registro(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
            case "compra":
                compra(obj, session);
                break;
            case "venta":
                venta(obj, session);
                break;
            case "traspaso":
                traspaso(obj, session);
                break;
            case "amortizarCuotaCompra":
                amortizarCuotaCompra(obj, session);
                break;
            case "anularVenta":
                anular(obj, session, "venta");
                break;
            case "anularCompra":
                anular(obj, session, "compra");
                break;
        }
    }

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

            JSONObject data = SocketCliente.sendSinc("compra_venta", obj);

            if (data.getString("estado").equals("error")) {
                throw new Exception(data.optString("error", "Error al anular la " + tipo + " en compra venta"));
            }

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

            conectInstance.commit();
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    

    public static void amortizarCuotaComprass(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee" + obj);

        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            String key_empresa = obj.getString("key_empresa");
            String key_caja = obj.getString("key_caja");
            String key_usuario = obj.getString("key_usuario");
            JSONArray cuotas = obj.optJSONArray("cuotas");

            JSONObject tiposPago = obj.getJSONObject("tipos_pago");
            System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee tipos_pago: " + tiposPago);

            JSONArray cajaDetalle = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(obj.getJSONObject("tipos_pago")).length; i++) {
                String key = JSONObject.getNames(obj.getJSONObject("tipos_pago"))[i];
                JSONObject value = obj.getJSONObject("tipos_pago").getJSONObject(key);
                JSONObject empresaTipoPago = EmpresaTipoPago.getByKey(key);
                empresaTipoPago.put("monto_nacional", value.optDouble("monto_nacional"));
                empresaTipoPago.put("monto_extranjera", value.optDouble("monto_extranjera"));

                double tipo_cambio = value.optDouble("monto_nacional")
                        / value.optDouble("monto_extranjera", value.optDouble("monto_nacional"));
                tipo_cambio = Math.round(tipo_cambio * 100.0) / 100.0;

                JSONObject det = new JSONObject();
                det.put("key", SUtil.uuid());
                det.put("key_caja", key_caja);
                det.put("key_empresa_tipo_pago", empresaTipoPago.getString("key"));
                det.put("key_tipo_pago", empresaTipoPago.getString("key_tipo_pago"));
                det.put("monto", value.getDouble("monto_extranjera"));
                det.put("key_moneda", empresaTipoPago.getString("key_moneda"));
                det.put("tipo_cambio", tipo_cambio);
                det.put("descripcion", obj.optString("descripcion"));
                det.put("tipo", "amortizacion_compra");
                det.put("fecha", SUtil.now());
                det.put("fecha_on", SUtil.now());
                det.put("estado", 1);
                det.put("key_usuario", key_usuario);
                // det.put("key_comprobante", key_comprobante);
                // det.put("codigo_comprobante", codigo_comprobante);
                // det.put("data", info);

                cajaDetalle.put(det);

            }

            obj.put("component", "compra_venta");
            JSONObject response = SocketCliente.sendSinc("compra_venta", obj);
            // System.out.println(response);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            // conectInstance.insertArray("caja_detalle", cajaDetalle);

            conectInstance.commit();
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static void amortizarCuotaCompra(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;

        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            String key_empresa = obj.getString("key_empresa");
            String key_caja = obj.getString("key_caja");
            String key_usuario = obj.getString("key_usuario");

            // 🚀 CORRECCIÓN AQUÍ
            JSONObject data = obj.getJSONObject("data");
            JSONObject tiposPago = data.getJSONObject("tipos_pago");
            JSONArray cuotas = data.optJSONArray("cuotas");

            System.out.println("cuotas: " + cuotas);
            System.out.println("tipos_pago: " + tiposPago);
            System.out.println("aaaaaaaaaas: " + obj);

            JSONArray cajaDetalle = new JSONArray();

            String[] names = JSONObject.getNames(tiposPago);

            for (String key : names) {

                JSONObject value = tiposPago.getJSONObject(key);
                JSONObject empresaTipoPago = EmpresaTipoPago.getByKey(key);

                
                value.put("key_cuenta_contable", empresaTipoPago.getString("key_cuenta_contable"));
                value.put("empresa_tipo_pago",empresaTipoPago);

                double tipo_cambio = value.optDouble("monto_nacional")
                        / value.optDouble("monto_extranjera", value.optDouble("monto_nacional"));
                tipo_cambio = Math.round(tipo_cambio * 100.0) / 100.0;

                JSONObject det = new JSONObject();
                det.put("key", SUtil.uuid());
                det.put("key_caja", key_caja);
                det.put("key_empresa_tipo_pago", empresaTipoPago.getString("key"));
                det.put("key_tipo_pago", empresaTipoPago.getString("key_tipo_pago"));
                det.put("monto", value.getDouble("monto_extranjera"));
                det.put("key_moneda", empresaTipoPago.getString("key_moneda"));
                det.put("tipo_cambio", tipo_cambio);
                det.put("descripcion", "Amortización de cuota de compra");
                // det.put("descripcion", obj.optString("descripcion"));
                // det.put("descripcion", data.optString("descripcion"));

                det.put("tipo", "amortizacion_compra");
                det.put("fecha", SUtil.now());
                det.put("fecha_on", SUtil.now());
                det.put("estado", 1);
                det.put("key_usuario", key_usuario);

                cajaDetalle.put(det);
            }

            conectInstance.insertArray("caja_detalle", cajaDetalle);

            // obj.put("data",
            // new JSONObject().put("cajaDetalle", cajaDetalle).put("tiposPago",
            // tiposPago).put("cuotas", cuotas));

          //  obj.put("data", cajaDetalle.getJSONObject(0).put("tipos_pago", tiposPago).put("cuotas", cuotas));

            System.out.println("obj: " + obj);

            obj.put("component", "compra_venta");
            JSONObject response = SocketCliente.sendSinc("compra_venta", obj);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            obj.put("estado", "exito");
            conectInstance.commit();

        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static void traspaso(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONObject data = obj.getJSONObject("data");
            String keyCaja = data.getString("key_caja");

            JSONObject caja = Caja.getByKey(keyCaja);
            data.put("caja", caja);

            System.out.println(data);

            String key_empresa_tipo_pago_origen = data.getString("key_empresa_tipo_pago_origen");
            String key_empresa_tipo_pago_destino = data.getString("key_empresa_tipo_pago_destino");
            double monto_origen = data.getDouble("monto_origen");
            double monto_destino = data.getDouble("monto_destino");

            JSONObject empresaTipoPagoOrigen = EmpresaTipoPago.getByKey(key_empresa_tipo_pago_origen);
            JSONObject empresaTipoPagoDestino = EmpresaTipoPago.getByKey(key_empresa_tipo_pago_destino);

            double tipo_cambio = monto_origen / monto_destino;
            tipo_cambio = Math.round(tipo_cambio * 100.0) / 100.0;

            JSONArray cajaDetalle = new JSONArray();

            JSONObject detalle1 = new JSONObject();
            detalle1.put("key", SUtil.uuid());
            detalle1.put("key_caja", keyCaja);
            detalle1.put("empresa_tipo_pago", empresaTipoPagoOrigen);
            detalle1.put("key_empresa_tipo_pago", empresaTipoPagoOrigen.getString("key"));
            detalle1.put("key_tipo_pago", empresaTipoPagoOrigen.getString("key_tipo_pago"));
            detalle1.put("tipo_pago", empresaTipoPagoOrigen);
            detalle1.put("monto", monto_origen * -1);
            detalle1.put("key_moneda", empresaTipoPagoOrigen.getString("key_moneda"));
            detalle1.put("tipo_cambio", tipo_cambio);
            detalle1.put("descripcion", data.optString("descripcion"));
            detalle1.put("tipo", "traspaso");
            detalle1.put("fecha", SUtil.now());
            detalle1.put("fecha_on", SUtil.now());
            detalle1.put("estado", 1);
            detalle1.put("key_usuario", data.getString("key_usuario"));
            // detalle1.put("key_comprobante", key_comprobante);
            // det.put("codigo_comprobante", codigo_comprobante);
            // det.put("data", info);

            JSONObject detalle2 = new JSONObject();
            detalle2.put("key", SUtil.uuid());
            detalle2.put("key_caja", keyCaja);
            detalle2.put("empresa_tipo_pago", empresaTipoPagoDestino);
            detalle2.put("key_empresa_tipo_pago", empresaTipoPagoDestino.getString("key"));
            detalle2.put("key_tipo_pago", empresaTipoPagoDestino.getString("key_tipo_pago"));
            detalle2.put("tipo_pago", empresaTipoPagoDestino);
            detalle2.put("monto", monto_destino);
            detalle2.put("key_moneda", empresaTipoPagoDestino.getString("key_moneda"));
            detalle2.put("tipo_cambio", tipo_cambio);
            detalle2.put("descripcion", data.optString("descripcion"));
            detalle2.put("tipo", "traspaso");
            detalle2.put("fecha", SUtil.now());
            detalle2.put("fecha_on", SUtil.now());
            detalle2.put("estado", 1);
            detalle2.put("key_usuario", data.getString("key_usuario"));

            cajaDetalle.put(detalle1);
            cajaDetalle.put(detalle2);

            data.put("caja_detalle", cajaDetalle);

            JSONObject send = new JSONObject();
            send.put("component", "asiento_contable");
            send.put("type", "traspaso_caja");
            send.put("data", data);

            JSONObject response = SocketCliente.sendSinc("contabilidad", send);
            // System.out.println(response);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            data = response.getJSONObject("data");

            for (int i = 0; i < cajaDetalle.length(); i++) {
                JSONObject detalle = cajaDetalle.getJSONObject(i);
                detalle.put("key_comprobante", data.getJSONObject("asiento_contable").getString("key"));
                detalle.put("codigo_comprobante", data.getJSONObject("asiento_contable").getString("codigo"));
            }

            conectInstance.insertArray("caja_detalle", cajaDetalle);

            conectInstance.commit();
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static void compra(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            // Inicio
            JSONObject data = obj.getJSONObject("data");
            String keyCaja = data.getString("key_caja");

            JSONObject caja = Caja.getByKey(keyCaja);
            data.put("caja", caja);

            String key_compra_venta = SUtil.uuid();

            JSONArray cajaDetalle = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                String key = JSONObject.getNames(data.getJSONObject("tipos_pago"))[i];
                JSONObject value = data.getJSONObject("tipos_pago").getJSONObject(key);

                // Si el tipo de pago es mayor a 0
                JSONObject empresaTipoPago = EmpresaTipoPago.getByKey(key);
                empresaTipoPago.put("monto_nacional", value.optDouble("monto_nacional"));
                empresaTipoPago.put("monto_extranjera", value.optDouble("monto_extranjera"));

                value.put("empresa_tipo_pago", empresaTipoPago);

                double tipo_cambio = value.optDouble("monto_nacional")
                        / value.optDouble("monto_extranjera", value.optDouble("monto_nacional"));
                tipo_cambio = Math.round(tipo_cambio * 100.0) / 100.0;

                JSONObject det = new JSONObject();
                det.put("key", key_compra_venta);
                det.put("key_caja", keyCaja);
                det.put("key_empresa_tipo_pago", empresaTipoPago.getString("key"));
                det.put("key_tipo_pago", empresaTipoPago.getString("key_tipo_pago"));
                det.put("tipo_pago", empresaTipoPago);
                det.put("monto", value.getDouble("monto_extranjera") * -1);
                det.put("key_moneda", empresaTipoPago.getString("key_moneda"));
                det.put("tipo_cambio", tipo_cambio);
                det.put("descripcion", data.optString("descripcion"));
                det.put("tipo", "compra");
                det.put("fecha", SUtil.now());
                det.put("fecha_on", SUtil.now());
                det.put("estado", 1);
                det.put("key_usuario", data.getString("key_usuario"));
                // det.put("key_comprobante", key_comprobante);
                // det.put("codigo_comprobante", codigo_comprobante);
                // det.put("data", info);

                cajaDetalle.put(det);

            }
            caja.put("detalle", cajaDetalle);

            // Se realiza la venta en compra venta

            data.put("key_compra_venta", key_compra_venta);

            JSONObject send = new JSONObject();
            send.put("component", "compra_venta");
            send.put("type", "compraCaja");
            send.put("data", data);

            JSONObject response = SocketCliente.sendSinc("compra_venta", send);
            // System.out.println(response);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            data = response.getJSONObject("data");

            JSONObject asientoContable = data.getJSONObject("asiento_contable");

            for (int i = 0; i < cajaDetalle.length(); i++) {
                JSONObject det = cajaDetalle.getJSONObject(i);
                det.put("key_comprobante", asientoContable.getString("key"));
                det.put("codigo_comprobante", asientoContable.getString("codigo"));
            }

            conectInstance.insertArray(COMPONENT, cajaDetalle);
            conectInstance.commit();
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static void venta(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            // Inicio
            JSONObject data = obj.getJSONObject("data");
            String keyCaja = data.getString("key_caja");

            JSONObject caja = Caja.getByKey(keyCaja);
            data.put("caja", caja);

            String key_compra_venta = SUtil.uuid();

            JSONArray cajaDetalle = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                String key = JSONObject.getNames(data.getJSONObject("tipos_pago"))[i];
                JSONObject value = data.getJSONObject("tipos_pago").getJSONObject(key);
                // Si el tipo de pago es mayor a 0
                JSONObject empresaTipoPago = EmpresaTipoPago.getByKey(key);
                empresaTipoPago.put("monto_nacional", value.optDouble("monto_nacional"));
                empresaTipoPago.put("monto_extranjera", value.optDouble("monto_extranjera"));

                value.put("empresa_tipo_pago", empresaTipoPago);

                double tipo_cambio = value.optDouble("monto_nacional")
                        / value.optDouble("monto_extranjera", value.optDouble("monto_nacional"));
                tipo_cambio = Math.round(tipo_cambio * 100.0) / 100.0;

                JSONObject det = new JSONObject();
                det.put("key", SUtil.uuid());
                det.put("key_caja", keyCaja);
                det.put("key_empresa_tipo_pago", empresaTipoPago.getString("key"));
                det.put("key_tipo_pago", empresaTipoPago.getString("key_tipo_pago"));
                det.put("monto", value.getDouble("monto_extranjera"));
                det.put("key_moneda", empresaTipoPago.getString("key_moneda"));
                det.put("tipo_cambio", tipo_cambio);
                det.put("descripcion", data.optString("descripcion"));
                det.put("tipo", "venta");
                det.put("fecha", SUtil.now());
                det.put("fecha_on", SUtil.now());
                det.put("estado", 1);
                det.put("key_usuario", data.getString("key_usuario"));
                det.put("key_compra_venta", key_compra_venta);
                // det.put("key_comprobante", key_comprobante);
                // det.put("codigo_comprobante", codigo_comprobante);
                // det.put("data", info);

                cajaDetalle.put(det);
            }

            caja.put("detalle", cajaDetalle);

            data.put("key_compra_venta", key_compra_venta);

            JSONObject send = new JSONObject();
            send.put("component", "compra_venta");
            send.put("type", "ventaCaja");
            send.put("data", data);

            JSONObject response = SocketCliente.sendSinc("compra_venta", send);
            // System.out.println(response);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            data = response.getJSONObject("data");

            JSONObject asientoContable = data.getJSONObject("asiento_contable");

            for (int i = 0; i < cajaDetalle.length(); i++) {
                JSONObject det = cajaDetalle.getJSONObject(i);
                det.put("key_comprobante", asientoContable.getString("key"));
                det.put("codigo_comprobante", asientoContable.getString("codigo"));
            }

            conectInstance.insertArray(COMPONENT, cajaDetalle);

            conectInstance.commit();
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }
    public static void venta2(JSONObject obj) {
        ConectInstance conectInstance = null;
        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            // Inicio
            JSONObject data = obj.getJSONObject("data");
            String keyCaja = data.getString("key_caja");

            JSONObject caja = Caja.getByKey(keyCaja);
            data.put("caja", caja);

            String key_compra_venta = SUtil.uuid();

            JSONArray cajaDetalle = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                String key = JSONObject.getNames(data.getJSONObject("tipos_pago"))[i];
                JSONObject value = data.getJSONObject("tipos_pago").getJSONObject(key);
                // Si el tipo de pago es mayor a 0
                JSONObject empresaTipoPago = EmpresaTipoPago.getByKey(key);
                empresaTipoPago.put("monto_nacional", value.optDouble("monto_nacional"));
                empresaTipoPago.put("monto_extranjera", value.optDouble("monto_extranjera"));

                value.put("empresa_tipo_pago", empresaTipoPago);

                double tipo_cambio = value.optDouble("monto_nacional")
                        / value.optDouble("monto_extranjera", value.optDouble("monto_nacional"));
                tipo_cambio = Math.round(tipo_cambio * 100.0) / 100.0;

                JSONObject det = new JSONObject();
                det.put("key", SUtil.uuid());
                det.put("key_caja", keyCaja);
                det.put("key_empresa_tipo_pago", empresaTipoPago.getString("key"));
                det.put("key_tipo_pago", empresaTipoPago.getString("key_tipo_pago"));
                det.put("monto", value.getDouble("monto_extranjera"));
                det.put("key_moneda", empresaTipoPago.getString("key_moneda"));
                det.put("tipo_cambio", tipo_cambio);
                det.put("descripcion", data.optString("descripcion"));
                det.put("tipo", "venta");
                det.put("fecha", SUtil.now());
                det.put("fecha_on", SUtil.now());
                det.put("estado", 1);
                det.put("key_usuario", data.getString("key_usuario"));
                det.put("key_compra_venta", key_compra_venta);
                // det.put("key_comprobante", key_comprobante);
                // det.put("codigo_comprobante", codigo_comprobante);
                // det.put("data", info);

                cajaDetalle.put(det);
            }

            caja.put("detalle", cajaDetalle);

            data.put("key_compra_venta", key_compra_venta);

            JSONObject send = new JSONObject();
            send.put("component", "compra_venta");
            send.put("type", "ventaCaja");
            send.put("data", data);

            JSONObject response = SocketCliente.sendSinc("compra_venta", send);
            // System.out.println(response);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            data = response.getJSONObject("data");

            JSONObject asientoContable = data.getJSONObject("asiento_contable");

            for (int i = 0; i < cajaDetalle.length(); i++) {
                JSONObject det = cajaDetalle.getJSONObject(i);
                det.put("key_comprobante", asientoContable.getString("key"));
                det.put("codigo_comprobante", asientoContable.getString("codigo"));
            }

            conectInstance.insertArray(COMPONENT, cajaDetalle);

            conectInstance.commit();
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all_caja_detalle('" + obj.getString("key_caja") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);

            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getMontoCaja(String key_caja) {
        try {
            String consulta = "select get_monto_caja('" + key_caja + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject Apertura(String key_punto_venta, String key_caja, ConectInstance conectInstance)
            throws Exception {

        JSONObject last = Caja.getLast(key_punto_venta);

        if (last == null) {
            return new JSONObject().put("monto", 0);
        }
        if (!last.has("key")) {
            return new JSONObject().put("monto", 0);
        }
        double monto = 0;
        JSONArray puntoVentaTipoPagoMontos = Caja.getMontoCajaTipoPago(last.getString("key"));

        JSONArray detalle = new JSONArray();
        for (int i = 0; i < puntoVentaTipoPagoMontos.length(); i++) {
            JSONObject item = puntoVentaTipoPagoMontos.getJSONObject(i);
            JSONObject moneda = Contabilidad.getMoneda(last.getString("key_empresa"), item.getString("key_moneda"));

            JSONObject empresatipoPago = EmpresaTipoPago.getByKey(item.getString("key_empresa_tipo_pago"));

            if (empresatipoPago == null)
                continue;
            if (!empresatipoPago.getString("key_tipo_pago").equals("caja"))
                continue;
            // Si el tipo de pago es mayor a 0

            if (item.getDouble("monto") > 0) {
                JSONObject det = new JSONObject();

                monto += item.getDouble("monto");
                det.put("key", SUtil.uuid());
                det.put("key_caja", key_caja);
                det.put("key_tipo_pago", empresatipoPago.getString("key_tipo_pago"));
                det.put("key_empresa_tipo_pago", empresatipoPago.getString("key"));
                det.put("key_moneda", item.getString("key_moneda"));
                det.put("tipo_cambio", moneda.getDouble("tipo_cambio"));
                det.put("monto", item.getDouble("monto"));
                det.put("descripcion", "Apertura de caja " + empresatipoPago.getString("key_tipo_pago"));
                det.put("tipo", "apertura");
                det.put("fecha", SUtil.now());
                det.put("fecha_on", SUtil.now());
                det.put("estado", 1);

                detalle.put(det);
            }
        }

        conectInstance.insertArray("caja_detalle", detalle);

        return new JSONObject().put("monto", monto);

    }

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + obj.getString("key") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getByKey(String key) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + key + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject getByKeyCompraVenta(String key_compra_venta) {
        try {
            String consulta = "select get_by('" + COMPONENT + "', 'key_compra_venta', '" + key_compra_venta
                    + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject getByKeyPuntoVenta(String key_punto_venta) {
        try {
            String consulta = "select get_abiertas_punto_venta('" + key_punto_venta + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void registro(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONObject data = obj.getJSONObject("data");

            if (data.has("key")) {
                data.put("key", data.getString("key"));
            } else {
                data.put("key", SUtil.uuid());
            }
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            JSONObject newData = new JSONObject(data.toString());

            conectInstance.insertArray(COMPONENT, new JSONArray().put(newData));
            switch (data.getString("tipo")) {
                case "ingreso":
                    Contabilidad.ingreso(obj, conectInstance);
                    break;
                case "ingreso_efectivo":
                    Contabilidad.ingreso_efectivo(obj);
                    break;
                // case "amortizacion": Contabilidad.amortizacion(obj); break;
                case "ingreso_banco":
                    Contabilidad.ingreso_banco(obj, conectInstance);
                    break;
                case "egreso_banco":
                    // obj.getJSONObject("data").put("monto",
                    // obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.egreso_banco(obj, conectInstance);
                    // obj.getJSONObject("data").put("monto",
                    // obj.getJSONObject("data").getDouble("monto")*-1);
                    break;
                case "egreso_efectivo":
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto") * -1);
                    Contabilidad.egreso_efectivo(obj, conectInstance);
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto") * -1);
                    break;

                case "pago_servicio":
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto") * -1);
                    Contabilidad.pago_servicio(obj);
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto") * -1);
                    break;
                default:
                    break;
            }
            conectInstance.commit();
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            conectInstance.rollback();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            SPGConect.editObject(COMPONENT, data);

            obj.put("data", data);
            obj.put("estado", "exito");

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
