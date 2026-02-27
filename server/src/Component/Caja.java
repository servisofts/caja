package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonObject;

import Contabilidad.Contabilidad;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;

public class Caja {
    public static final String COMPONENT = "caja";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getAllCajasByEmpresa":
                getAllCajasByEmpresa(obj, session);
                break;
            case "getAllMovimientosCajasByEmpresa":
                getAllMovimientosCajasByEmpresa(obj, session);
                break;
            case "getActiva":
                getActiva(obj, session);
                break;
            case "getHistorico":
                getHistorico(obj, session);
                break;
            case "getLast":
                getLast(obj, session);
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
            case "reporteCuentas":
                reporteCuentas(obj, session);
                break;
            case "getAutomatica":
                getAutomatica(obj, session);
                break;

        }
    }

    public static void getAllCajasByEmpresa(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select _getAllCajasByEmpresa('" + obj.getString("key_empresa") + "','"
                    + obj.getString("fecha_inicio") + "','" + obj.getString("fecha_fin") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");

            e.printStackTrace();
        }
    }

    public static void getAllMovimientosCajasByEmpresa(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select _getallmovimientoscajasbyempresa('" + obj.getString("key_empresa") + "','"
                    + obj.getString("fecha_inicio") + "','" + obj.getString("fecha_fin") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_abiertas('" + obj.getString("key_empresa") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getActiva(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_abiertas('" + obj.getString("key_empresa") + "', '"
                    + obj.getString("key_usuario") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getHistorico(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_historico_caja('" + obj.getJSONObject("servicio").getString("key") + "', '"
                    + obj.getString("fecha_inicio") + "','" + obj.getString("fecha_fin") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void reporteCuentas(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select reporte_cuentas('" + obj.getJSONObject("servicio").getString("key") + "', '"
                    + obj.getString("fecha_inicio") + "', '" + obj.getString("fecha_fin") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getLast(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_last('" + obj.getString("key_punto_venta") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getLast(String key_punto_venta) {
        try {
            String consulta = "select get_ultima_caja('" + key_punto_venta + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void getPuntosVenta(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_abiertas('" + obj.getJSONObject("servicio").getString("key") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "','" + obj.getString("key") + "') as json";
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

            JSONObject cajas = getByKeyPuntoVenta(data.getString("key_punto_venta"));

            if (!cajas.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "Caja abierta");
                obj.put("data", cajas);
                return;
            }

            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            data.put("key_servicio", obj.getJSONObject("servicio").getString("key"));
            conectInstance.insertArray(COMPONENT, new JSONArray().put(data));

            JSONObject apertura = CajaDetalle.Apertura(data.getString("key_punto_venta"), data.getString("key"),
                    conectInstance);

            Notificar.send("💻 Abriste una caja", "Monto de apertura Bs. " + apertura.getDouble("monto"), data,
                    obj.getJSONObject("servicio").getString("key"), obj.getString("key_usuario"));

            obj.put("data", data);
            obj.put("estado", "exito");
            conectInstance.commit();
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
            e.printStackTrace();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static JSONArray getMontoCajaTipoPago(String key_caja) {
        try {

            return SPGConect.ejecutarConsultaArray("select get_monto_caja_tipo_pago('" + key_caja + "') as json");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");

            if (obj.has("action") && obj.getString("action").equals("cerrar")) {
                Contabilidad.caja_cierre(obj);
            }

            if (obj.optString("estado").equals("error")) {
                return;
            }
            // Notificar.send("💻 Cerraste una caja", "Monto de cierre Bs. ", data,
            // obj.getJSONObject("servicio").getString("key"),
            // obj.getString("key_usuario"));

            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getAutomatica(JSONObject obj, SSSessionAbstract session) {
        try {
            String key_punto_venta = obj.getString("key_punto_venta");
            JSONObject cajas = getByKeyPuntoVenta(key_punto_venta);

            if (!cajas.isEmpty()) {
                JSONObject caja = cajas.getJSONObject(cajas.keys().next());
                if (caja.getInt("estado") == 1) {
                    obj.put("estado", "exito");
                    obj.put("data", caja);
                    return;
                }
            }

            JSONObject puntoVenta = getPuntoVentaByKey(key_punto_venta);

            if (puntoVenta.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "Punto de venta no encontrado");
                return;
            }

            JSONObject sucursal = sucursalGetByKey(puntoVenta.getString("key_sucursal"));

            if (sucursal.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "Sucursal no encontrada");
                return;
            }
            JSONObject objSend = new JSONObject();
            objSend.put("servicio", obj.optJSONObject("servicio"));
            objSend.put("key_usuario", "");
            JSONObject data = new JSONObject();
            data.put("key_punto_venta", key_punto_venta);
            data.put("key_sucursal", sucursal.getString("key"));
            data.put("key_empresa", sucursal.getString("key_empresa"));
            data.put("fecha", SUtil.now().substring(0, 10));
            data.put("fraccionar_moneda", false);
            objSend.put("data", data);

            registro(objSend, session);

            System.out.println(objSend.toString());

            // debo abrir una casa

            obj.put("data", objSend.getJSONObject("data"));
            obj.put("estado", "exito");

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getPuntoVentaByKey(String key_punto_venta) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta");
        send.put("type", "getByKey");
        send.put("key", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject sucursalGetByKey(String key_sucursal) {
        JSONObject send = new JSONObject();
        send.put("component", "sucursal");
        send.put("type", "getByKey");
        send.put("key", key_sucursal);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }
}
