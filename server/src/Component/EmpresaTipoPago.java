package Component;

import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;

import java.sql.SQLException;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class EmpresaTipoPago {

    public final static String COMPONENT = "empresa_tipo_pago";

    public EmpresaTipoPago(JSONObject obj, SSSessionAbstract session) {
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

    public void getAll(JSONObject obj, SSSessionAbstract session) {
        try {

            String consulta = "select get_all('" + COMPONENT + "', 'key_empresa', '" + obj.getString("key_empresa")
                    + "') as json";
            if (obj.has("key_punto_venta")) {
                consulta = "select get_all_empresa_tipo_pago_punto_venta('" + obj.getString("key_empresa") + "', '"
                        + obj.getString("key_punto_venta") + "') as json";
            }
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (SQLException e) {
            obj.put("estado", "error");
            e.printStackTrace();
        }
    }

    public static JSONObject getAll(String key_empresa) {
        try {

            String consulta = "select get_all('" + COMPONENT + "', 'key_empresa', '" + key_empresa
                    + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "','" + obj.getString("key") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (SQLException e) {
            obj.put("estado", "error");
            e.printStackTrace();
        }
    }

    public static JSONObject getByKey(String key) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "','" + key + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void registro(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            data.put("key", UUID.randomUUID().toString());
            data.put("fecha_on", SUtil.now());
            data.put("estado", 1);
            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));

            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (SQLException e) {
            obj.put("estado", "error");
            e.printStackTrace();
        }
    }

    public void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (SQLException e) {
            obj.put("estado", "error");
            obj.put("error", e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}