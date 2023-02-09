import Servisofts.Servisofts;

public class App {
    public static void main(String[] args) {
        try {
            Servisofts.DEBUG = false;
            Servisofts.ManejadorCliente = ManejadorCliente::onMessage;
            Servisofts.Manejador = Manejador::onMessage;
            Servisofts.initialize(); 
            //PDF.prueba("asd", "·FF0000");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}