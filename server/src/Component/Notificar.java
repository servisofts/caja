package Component;

import org.json.JSONObject;

import SocketCliente.SocketCliente;

public class Notificar {
    
    public static void send(String descripcion, String observacion, JSONObject data, String key_servicio, String key_usuario){
        try{
            JSONObject notificar = new JSONObject();
            notificar.put("component", "notification");
            notificar.put("type", "registro");
            notificar.put("key_usuario", key_usuario);
            JSONObject not_data = new JSONObject();
            not_data.put("descripcion", descripcion);
            not_data.put("observacion", observacion);
            not_data.put("tipo", "compra_venta");
            not_data.put("key_servicio", key_servicio);
            not_data.put("data", data);
            notificar.put("data", not_data);
            
            SocketCliente.send("notification", notificar.toString()); 
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
