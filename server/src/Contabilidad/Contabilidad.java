package Contabilidad;

import org.json.JSONArray;
import org.json.JSONObject;
import Component.Caja;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;
import Util.ConectInstance;

public class Contabilidad {

    public static JSONObject getMoneda(String key_empresa, String key_moneda){
        JSONObject send = new JSONObject();
        send.put("service", "empresa");
        send.put("component", "empresa_moneda");
        send.put("type", "getAll");
        send.put("key_empresa", key_empresa);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("key").equals(key_moneda)) {
                return item;
            }
        }
        return resp;
    }

    public static JSONObject getMonedaBase(String key_empresa){
        JSONObject send = new JSONObject();
        send.put("service", "empresa");
        send.put("component", "empresa_moneda");
        send.put("type", "getAll");
        send.put("key_empresa", key_empresa);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("tipo").equals("base")) {
                return item;
            }
        }
        return resp;
    }

    public static JSONObject getAjusteEmpresa(String key_empresa, String key_ajuste) {
        JSONObject send = new JSONObject();
        send.put("component", "ajuste_empresa");
        send.put("type", "getByKeyAjuste");
        send.put("key_empresa", key_empresa);
        send.put("key_ajuste", key_ajuste);
        JSONObject resp = SocketCliente.sendSinc("contabilidad", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject getTipoPago(String key_tipo_pago) {
        JSONObject send = new JSONObject();
        send.put("component", "tipo_pago");
        send.put("type", "getByKey");
        send.put("key", key_tipo_pago);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject puntoVentaTipoPago(String key_punto_venta, String key_tipo_pago) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta_tipo_pago");
        send.put("type", "getAll");
        send.put("key_punto_venta", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("key_tipo_pago").equals(key_tipo_pago)) {
                return item;
            }
        }
        return resp;
    }
   
    public static JSONObject puntoVentaTipoPago(String key_punto_venta, String key_tipo_pago, String key_moneda) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta_tipo_pago");
        send.put("type", "getAll");
        send.put("key_punto_venta", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("key_tipo_pago").equals(key_tipo_pago) && item.getString("key_moneda").equals(key_moneda)) {
                return item;
            }
        }
        return resp;
    }
   


    public static void ingreso(JSONObject obj, ConectInstance conectInstance) throws Exception{
        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        JSONObject send = new JSONObject();

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "ingreso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja ingreso de efectivo: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        

        // Sale de la cuenta que me pasa el frontend por el haber
        det = new JSONObject();
        det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        String keyTipoPago=obj.getJSONObject("data").getString("key_tipo_pago");

        JSONObject tipoPago = Contabilidad.getTipoPago(keyTipoPago);
        JSONObject puntoVentaTipoPago = Contabilidad.puntoVentaTipoPago(caja.getString("key_punto_venta"), keyTipoPago);


        if(tipoPago.optBoolean("pasa_por_caja")){
            // Entra a caja por el debe
            det = new JSONObject();
            det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("debe", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);
        }else{
            // Entra a bancos por el debe
            det = new JSONObject();
            det.put("key_cuenta_contable", puntoVentaTipoPago.getString("key_cuenta_contable"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("debe", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);
        }

        comprobante.put("detalle", detalle);

        send.put("data", comprobante);
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        if(data.getString("estado").equals("error")){
            throw new Exception(data.getString("error"));
        }

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        conectInstance.editObject("caja_detalle", caja_detalle_);
       

    }
    public static void caja_cierre(JSONObject obj) throws Exception{

        obj.getJSONObject("data").put("monto_cierre", 0);
        obj.getJSONObject("data").put("fecha_cierre", SUtil.now());

        SPGConect.editObject("caja", obj.getJSONObject("data"));
        //JSONObject send = new JSONObject();
        //send.put("component", "asiento_contable");
        //send.put("type", "set");
        //send.put("key_usuario", "");
        //send.put("key_empresa", obj.getString("key_empresa"));
//
        //obj.getJSONObject("data").put("fecha_cierre", SUtil.now());
//
        //JSONObject comprobante = new JSONObject();
        //comprobante.put("tipo", "traspaso");
        //comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        //
        //if(obj.getJSONObject("data").has("descripcion")){
        //    comprobante.put("descripcion", "Cierre de caja"+obj.getJSONObject("data").getString("descripcion"));
        //}else{
        //    comprobante.put("descripcion", "Cierre de caja");
        //}
        //
        //comprobante.put("observacion", "Cierre de caja");

        //JSONArray detalle = new JSONArray();



        //JSONArray puntoVentaTipoPagoMontos = Caja.getMontoCajaTipoPago(obj.getJSONObject("data").getString("key"));
        //JSONObject puntoVentaTipoPagos =Contabilidad.puntoVentaTipoPago(obj.getJSONObject("data").getString("key_punto_venta"),null);
        //JSONObject puntoVentaTipoPagos = obj.getJSONObject("punto_venta_tipo_pago");
        //JSONObject puntoVentaTipoPago;

        //cuenta contable caja
        //String keyCuentaContableCaja = obj.getJSONObject("data").getString("key_cuenta_contable");
        
        //String keyCuentaContableBanco;
        //double monto;
        //double monto_cierre = 0;
        //for (int i = 0; i < JSONObject.getNames(puntoVentaTipoPagos).length; i++) {
        //    puntoVentaTipoPago = puntoVentaTipoPagos.getJSONObject(JSONObject.getNames(puntoVentaTipoPagos)[i]);
        //    String keyTipoPago=puntoVentaTipoPago.getString("key_tipo_pago");
//
        //    JSONObject tipoPago = Contabilidad.getTipoPago(keyTipoPago);
        //    if(tipoPago.optBoolean("pasa_por_caja")){
        //        // manda a bancos
        //        if(puntoVentaTipoPago.has("key_cuenta_contable")){
        //            //keyCuentaContableBanco = puntoVentaTipoPago.getString("key_cuenta_contable");
        //            if(puntoVentaTipoPagoMontos.length() > 0){
        //                for (int j = 0; j < puntoVentaTipoPagoMontos.length(); j++) {
        //                    if(puntoVentaTipoPagoMontos.getJSONObject(j).getString("key_tipo_pago").equals(puntoVentaTipoPago.getString("key_tipo_pago"))){
        //                        monto = puntoVentaTipoPagoMontos.getJSONObject(j).getDouble("monto");
        //                        monto_cierre+=monto;
        //                        break;
        //                    }
        //                }
        //            }
//
        //                //JSONObject det = new JSONObject();
        //                //det.put("key_cuenta_contable", keyCuentaContableBanco);
        //                //det.put("glosa", "Cierre de caja "+puntoVentaTipoPago.getString("key_tipo_pago"));
        //                //det.put("debe", monto);
        //                //detalle.put(det);
        //        
        //        
        //                // Sale de la cuenta que me pasa el frontend por el haber
        //                //det = new JSONObject();
        //                //det.put("key_cuenta_contable", keyCuentaContableCaja);
        //                //det.put("glosa", "Cierre de caja "+puntoVentaTipoPago.getString("key_tipo_pago"));
        //                //det.put("haber", monto);
        //                //detalle.put(det);
        //            
        //        }
        //        
        //    }
        //}

        
      
        //comprobante.put("detalle", detalle);
        //send.put("data", comprobante);
        //JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        //if(data.getString("estado").equals("error")){
        //    obj.put("estado", "error");
        //    obj.put("error", data.getString("error"));
        //    return;
        //}

        //monto_cierre=Math.round(monto_cierre * 100.0) / 100.0;
//
        //if(monto_cierre<0){
        //    throw new Exception("Su caja esta en negativo");
        //}

        //obj.getJSONObject("data").put("key_comprobante_cierre", data.getJSONObject("data").getString("key"));
        //obj.getJSONObject("data").put("monto_cierre", 0);
        //obj.getJSONObject("data").put("fecha_cierre", SUtil.now());
//
        //SPGConect.editObject("caja", obj.getJSONObject("data"));

    }
    public static void ingreso_banco(JSONObject obj, ConectInstance conectInstance) throws Exception{

        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        String keyTipoPago=obj.getJSONObject("data").getString("key_tipo_pago");
        String keyMoneda=obj.getJSONObject("data").optString("key_moneda", null);

        JSONObject moneda;
        if(keyMoneda == null){
            moneda = Contabilidad.getMonedaBase(caja.getString("key_empresa"));
            keyMoneda = moneda.getString("key");
        }else{
            moneda = Contabilidad.getMoneda(caja.getString("key_empresa"), keyMoneda);
        }

        JSONObject tipoPago = Contabilidad.getTipoPago(keyTipoPago);

        if(!tipoPago.optBoolean("pasa_por_caja", false)){
            throw new Exception("El tipo de pago no pasa por caja");
        }


        JSONObject puntoVentaTipoPago = Contabilidad.puntoVentaTipoPago(caja.getString("key_punto_venta"), keyTipoPago, keyMoneda);
        System.out.println(moneda);

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");// Cambiar
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja ingreso de bancos: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();
        
        double monto = obj.getJSONObject("data").getDouble("monto");
        monto = monto * moneda.optDouble("tipo_cambio", 1);

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", puntoVentaTipoPago.getString("key_cuenta_contable"));
        det.put("glosa", "Caja ingreso de bancos");
        det.put("debe", monto);

        det.put("tags", 
            new JSONObject()
            .put("key_usuario", obj.getString("key_usuario"))
            .put("key_punto_venta", caja.getString("key_punto_venta"))
            .put("key_caja", caja.getString("key"))
            .put("key_sucursal", caja.getString("key_sucursal"))
        );
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", caja_detalle.getString("key_cuenta_banco"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", monto);
        det.put("tags", 
            new JSONObject()
            .put("key_usuario", obj.getString("key_usuario"))
            .put("key_punto_venta", caja.getString("key_punto_venta"))
            .put("key_caja", caja.getString("key"))
            .put("key_sucursal", caja.getString("key_sucursal"))
        );
        detalle.put(det);
        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        if(data.has("estado") && data.getString("estado").equals("error")){
            throw new Exception(data.getString("error"));
        }

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        caja_detalle_.put("key_moneda", moneda.optString("key", ""));
        caja_detalle_.put("tipo_cambio", moneda.optDouble("tipo_cambio", 1));

        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));
        conectInstance.editObject("caja_detalle", caja_detalle_);

    }
    public static void egreso_banco(JSONObject obj, ConectInstance conectInstance) throws Exception{
        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));
        JSONObject tipoPago = Contabilidad.getTipoPago(caja_detalle.getString("key_tipo_pago"));
        String keyMoneda=obj.getJSONObject("data").optString("key_moneda", null);

        JSONObject moneda;
        if(keyMoneda == null){
            moneda = Contabilidad.getMonedaBase(caja.getString("key_empresa"));
            keyMoneda = moneda.getString("key");
        }else{
            moneda = Contabilidad.getMoneda(caja.getString("key_empresa"), keyMoneda);
        }


        if(!tipoPago.optBoolean("pasa_por_caja", false)){
            throw new Exception("El tipo de pago no pasa por caja");
        }


        System.out.println(moneda);

        if(!tipoPago.optBoolean("pasa_por_caja", false)){
            throw new Exception("El tipo de pago no pasa por caja");
        }

        JSONArray cajaTipoPago = Caja.getMontoCajaTipoPago(caja_detalle.getString("key_caja"));

        double monto = 0;
        for (int i = 0; i < cajaTipoPago.length(); i++) {
            JSONObject item = cajaTipoPago.getJSONObject(i);
            if (item.getString("key_tipo_pago").equals(caja_detalle.getString("key_tipo_pago"))) {
                monto = item.optDouble("monto");
                break;
            }
        }

        double montoFrontend=obj.getJSONObject("data").getDouble("monto");
        montoFrontend = Math.round(montoFrontend * 100.0) / 100.0;
        monto = Math.round(monto * 100.0) / 100.0;
        
        if (monto < montoFrontend) {
            throw new Exception("No tiene suficiente monto para enviar");
        }


        JSONObject puntoVentaTipoPago = Contabilidad.puntoVentaTipoPago(caja.getString("key_punto_venta"), caja_detalle.getString("key_tipo_pago"), keyMoneda);

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");// Cambiar
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja egreso de bancos: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        det.put("tags", 
            new JSONObject()
            .put("key_usuario", obj.getString("key_usuario"))
            .put("key_punto_venta", caja.getString("key_punto_venta"))
            .put("key_caja", caja.getString("key"))
            .put("key_sucursal", caja.getString("key_sucursal"))
        );
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", puntoVentaTipoPago.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        det.put("tags", 
            new JSONObject()
            .put("key_usuario", obj.getString("key_usuario"))
            .put("key_punto_venta", caja.getString("key_punto_venta"))
            .put("key_caja", caja.getString("key"))
            .put("key_sucursal", caja.getString("key_sucursal"))
        );
        detalle.put(det);
        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));
        conectInstance.editObject("caja_detalle", caja_detalle_);

    }

    public static void ingreso_efectivo(JSONObject obj) throws Exception{

        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "ingreso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja ingreso de efectivo: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);
        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));
        SPGConect.editObject("caja_detalle", caja_detalle_);

    }
    public static void egreso_efectivo(JSONObject obj, ConectInstance conectInstance) throws Exception{
        
        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "egreso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja egreso de efectivo: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);
        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));
        conectInstance.editObject("caja_detalle", caja_detalle_);

    }
    public static void pago_servicio(JSONObject obj) throws Exception{
        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "egreso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja pago de servicio: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);


        JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(obj.getString("key_empresa"), "cuentas_por_pagar");

        det = new JSONObject();
        det.put("codigo", ajusteEmpresa.getString("codigo"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        // esta parte solo cuando se envia la plata a bancos

        //if(!obj.getJSONObject("data").getBoolean("enviar_cierre_caja")){
            // Entra a bancos por el debe
            det = new JSONObject();
            det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable_banco"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("debe", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);

            // Sale de caja por el haber
            det = new JSONObject();
            det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("haber", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);
        //}


        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);
        

        if(data.has("estado") && data.getString("estado").equals("error")){
            throw new Exception(data.getString("error"));
        }

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));


        SPGConect.editObject("caja_detalle", caja_detalle_);

    }
    
    public static void amortizacion(JSONObject obj) throws Exception{
        if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "ingreso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja cobro de servicio: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();


        JSONObject det = new JSONObject();


        JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(obj.getString("key_empresa"), "cuentas_por_pagar");
        det = new JSONObject();
        det.put("codigo", ajusteEmpresa.getString("codigo"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        // esta parte solo cuando se envia la plata a bancos

        if(!obj.getJSONObject("data").getBoolean("enviar_cierre_caja")){
            // Sale de caja por el haber
            det = new JSONObject();
            det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("haber", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);

            // Entra a bancos por el debe
            det = new JSONObject();
            det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable_banco"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("debe", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);
        }

        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        if(data.has("estado") && data.getString("estado").equals("error")){
            throw new Exception(data.getString("error"));
        }

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));


        SPGConect.editObject("caja_detalle", caja_detalle_);
        

    }


    
}
