package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Contabilidad.Contabilidad;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;
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
            
            JSONObject data = new JSONObject();
            data.put("key", SUtil.uuid());
            data.put("key_caja", key_caja);
            data.put("fecha_on", SUtil.now());
            data.put("estado", 1);
            data.put("descripcion", "Movimiento de apertura");
            data.put("monto", 0);

            //if(last != null && !last.isEmpty()) data.put("monto", last.getDouble("monto_cierre"));
            data.put("key_tipo_pago", "efectivo");
            data.put("tipo", "apertura");

            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));

            return data;
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
        try {

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
            

            switch (data.getString("tipo")) {
                case "ingreso": Contabilidad.ingreso(obj); break;
                case "ingreso_efectivo": Contabilidad.ingreso_efectivo(obj); break;
                case "amortizacion": Contabilidad.amortizacion(obj); break;
                case "ingreso_banco": Contabilidad.ingreso_banco(obj); break;
                case "egreso_banco": 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.egreso_banco(obj); 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                break;
                case "egreso_efectivo": 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.egreso_efectivo(obj); 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                break;

                case "pago_servicio": 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                    Contabilidad.pago_servicio(obj); 
                    obj.getJSONObject("data").put("monto", obj.getJSONObject("data").getDouble("monto")*-1);
                break;
                default: break;
            }
            SPGConect.insertArray(COMPONENT, new JSONArray().put(newData));
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
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
