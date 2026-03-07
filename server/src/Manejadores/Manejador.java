package Manejadores;
import Component.*;
import Servisofts.SConsole;
import org.json.JSONObject;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class Manejador {
    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        if (session != null) {
            SConsole.log(session.getIdSession(), "\t|\t", obj.getString("component"), obj.getString("type"));
        } else {
            SConsole.log("http-server", "-->", obj.getString("component"), obj.getString("type"));
        }
        if (obj.isNull("component")) {
            return;
        }
        switch (obj.getString("component")) {
            case Caja.COMPONENT: Caja.onMessage(obj, session); break;
            case CajaDetalle.COMPONENT: CajaDetalle.onMessage(obj, session); break;
            case CajaDetalleMoneda.COMPONENT: CajaDetalleMoneda.onMessage(obj, session); break;
            case TipoPago.COMPONENT: new TipoPago(obj, session); break;
            case EmpresaTipoPago.COMPONENT: new EmpresaTipoPago(obj, session); break;
            case EmpresaTipoPagoPuntoVenta.COMPONENT: new EmpresaTipoPagoPuntoVenta(obj, session); break;
            case Pasarela.COMPONENT: new Pasarela(obj, session); break;
            case PasarelaEmpresa.COMPONENT: new PasarelaEmpresa(obj, session); break;
            case Recurrente.COMPONENT: new Recurrente(obj, session); break;
            case Cotizacion.COMPONENT: new Cotizacion(obj, session); break;
        }
    }
}
