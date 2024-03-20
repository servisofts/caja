package Contabilidad;

import org.json.JSONArray;
import org.json.JSONObject;
import Component.Caja;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;

public class Contabilidad {
    public static JSONObject getAjusteEmpresa(String key_empresa, String key_ajuste) {
        JSONObject send = new JSONObject();
        send.put("component", "ajuste_empresa");
        send.put("type", "getByKeyAjuste");
        send.put("key_empresa", key_empresa);
        send.put("key_ajuste", key_ajuste);
        JSONObject resp = SocketCliente.sendSinc("contabilidad", send);
        return resp.getJSONObject("data");
    }

    public static void ingreso(JSONObject obj) throws Exception{
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

        // Entra a caja por el debe
        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);


        // Sale de la cuenta que me pasa el frontend por el haber
        det = new JSONObject();
        det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);


        // esta parte solo cuando se envia la plata a bancos

        if(!obj.getJSONObject("data").getBoolean("enviar_cierre_caja")){
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
        }



        
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
    public static void caja_cierre(JSONObject obj) throws Exception{

        
        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        obj.getJSONObject("data").put("fecha_cierre", SUtil.now());

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        
        if(obj.getJSONObject("data").has("descripcion")){
            comprobante.put("descripcion", "Cierre de caja"+obj.getJSONObject("data").getString("descripcion"));
        }else{
            comprobante.put("descripcion", "Cierre de caja");
        }
        
        comprobante.put("observacion", "Cierre de caja");

        JSONArray detalle = new JSONArray();



        JSONObject puntoVentaTipoPagoMontos = Caja.getMontoCajaTipoPago(obj.getJSONObject("data").getString("key"));
        JSONObject puntoVentaTipoPagos = obj.getJSONObject("punto_venta_tipo_pago");
        JSONObject puntoVentaTipoPago;

        //cuenta contable caja
        String keyCuentaContableCaja = obj.getJSONObject("data").getString("key_cuenta_contable");

        String keyCuentaContableBanco;
        double monto;
        double monto_cierre = 0;
        for (int i = 0; i < JSONObject.getNames(puntoVentaTipoPagos).length; i++) {
            puntoVentaTipoPago = puntoVentaTipoPagos.getJSONObject(JSONObject.getNames(puntoVentaTipoPagos)[i]);

            if(puntoVentaTipoPago.has("enviar_cierre_caja") && !puntoVentaTipoPago.isNull("enviar_cierre_caja") && puntoVentaTipoPago.getBoolean("enviar_cierre_caja")){
                // manda a bancos
                if(puntoVentaTipoPago.has("key_cuenta_contable")){
                    keyCuentaContableBanco = puntoVentaTipoPago.getString("key_cuenta_contable");
                    if(puntoVentaTipoPagoMontos.has(puntoVentaTipoPago.getString("key_tipo_pago"))){
                        monto = puntoVentaTipoPagoMontos.getJSONObject(puntoVentaTipoPago.getString("key_tipo_pago")).getDouble("monto");
                        monto_cierre+=monto;

                        JSONObject det = new JSONObject();
                        det.put("key_cuenta_contable", keyCuentaContableBanco);
                        det.put("glosa", "Cierre de caja "+puntoVentaTipoPago.getString("key_tipo_pago"));
                        det.put("debe", monto);
                        detalle.put(det);
                
                
                        // Sale de la cuenta que me pasa el frontend por el haber
                        det = new JSONObject();
                        det.put("key_cuenta_contable", keyCuentaContableCaja);
                        det.put("glosa", "Cierre de caja "+puntoVentaTipoPago.getString("key_tipo_pago"));
                        det.put("haber", monto);
                        detalle.put(det);
                    }
                }
                
            }
        }

        
        if(monto_cierre<=0){
            return;
        }

        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        if(data.getString("estado").equals("error")){
            obj.put("estado", "error");
            obj.put("error", data.getString("error"));
            return;
        }

        obj.getJSONObject("data").put("key_comprobante_cierre", data.getJSONObject("data").getString("key"));
        obj.getJSONObject("data").put("monto_cierre", monto_cierre);

        SPGConect.editObject("caja", obj.getJSONObject("data"));

    }
    public static void ingreso_banco(JSONObject obj) throws Exception{

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
        comprobante.put("tipo", "traspaso");// Cambiar
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja ingreso de bancos: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_banco"));
        det.put("glosa", "Caja ingreso de bancos");
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
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
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));
        SPGConect.editObject("caja_detalle", caja_detalle_);

    }
    public static void egreso_banco(JSONObject obj) throws Exception{
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
        comprobante.put("tipo", "traspaso");// Cambiar
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja egreso de bancos: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();

        JSONObject det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_banco"));
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
        SPGConect.editObject("caja_detalle", caja_detalle_);

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
    public static void egreso_efectivo(JSONObject obj) throws Exception{
        
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
        SPGConect.editObject("caja_detalle", caja_detalle_);

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

        if(!obj.getJSONObject("data").getBoolean("enviar_cierre_caja")){
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
