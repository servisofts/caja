package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Contabilidad.Contabilidad;
import Servisofts.SPG;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;
import Util.ConectInstance;
import Server.SSSAbstract.SSSessionAbstract;

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

    public static JSONObject Apertura(String key_punto_venta, String key_caja) {
        try {

            JSONObject last = Caja.getLast(key_punto_venta);

            if(last == null){
                  return new JSONObject().put("monto", 0);
            }
            if(!last.has("key")){
                return new JSONObject().put("monto", 0);
            }
            double monto=0;
            JSONArray puntoVentaTipoPagoMontos = Caja.getMontoCajaTipoPago(last.getString("key"));
            JSONArray detalle = new JSONArray();
            for (int i = 0; i < puntoVentaTipoPagoMontos.length(); i++) {
                JSONObject item = puntoVentaTipoPagoMontos.getJSONObject(i);
                JSONObject moneda = Contabilidad.getMoneda(last.getString("key_empresa"), item.getString("key_moneda"));

                JSONObject tipoPago = Contabilidad.getTipoPago(item.getString("key_tipo_pago"));

                if(item.getDouble("monto")>0 && tipoPago.optBoolean("pasa_por_caja",false)){
                    JSONObject det = new JSONObject();
                    monto+=item.getDouble("monto");
                    det.put("key", SUtil.uuid());
                    det.put("key_caja", key_caja);
                    det.put("key_tipo_pago", item.getString("key_tipo_pago"));
                    det.put("key_moneda", item.getString("key_moneda"));
                    det.put("tipo_cambio", moneda.getDouble("tipo_cambio"));
                    det.put("monto", item.getDouble("monto"));
                    det.put("descripcion", "Apertura de caja "+item.getString("key_tipo_pago"));
                    det.put("tipo", "apertura");
                    det.put("fecha", SUtil.now());
                    det.put("fecha_on", SUtil.now());
                    det.put("estado", 1);

                    detalle.put(det);
                }
            }

            SPGConect.insertArray("caja_detalle", detalle);

            return new JSONObject().put("monto", monto);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '"+obj.getString("key")+"') as json";
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

            if(data.has("key")){
                data.put("key", data.getString("key"));    
            }else{
                data.put("key", SUtil.uuid());
            }
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            JSONObject newData = new JSONObject(data.toString());
            
            conectInstance.insertArray(COMPONENT, new JSONArray().put(newData));
            switch (data.getString("tipo")) {
                case "ingreso": Contabilidad.ingreso(obj, conectInstance); break;
                case "ingreso_efectivo": Contabilidad.ingreso_efectivo(obj); break;
                //case "amortizacion": Contabilidad.amortizacion(obj); break;
                case "ingreso_banco": Contabilidad.ingreso_banco(obj, conectInstance); break;
                case "egreso_banco": 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.egreso_banco(obj, conectInstance); 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                break;
                case "egreso_efectivo": 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.egreso_efectivo(obj, conectInstance); 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                break;

                case "pago_servicio": 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.pago_servicio(obj); 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                break;
                default: break;
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
