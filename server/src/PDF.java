
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PDF {
    private static int page_num=1;
    private static PDRectangle mediaBox;
    private static PDType0Font font;
    private static String rutaFont;
    private static String rutaFontBold;
    private static int fontSize;
    private static PDDocument document;
    private static PDPage page;
    private static PDPageContentStream contentStream;
    private static int espacios[];
    private static int ANCHO = 612;
    private static int ALTO = 792;
    private static int ANCHO_MARGEN = 572;
    private static  int ALTO_MARGEN = 752;
    private static NumeroLiteral numero_literal;
    private static String pattern = "###,###,###.##";
    private static String pattern3 = "###,###,###.###";
    private static DecimalFormat myFormatter = new DecimalFormat(pattern,DecimalFormatSymbols.getInstance(Locale.US));
    private static DecimalFormat myFormatter3 = new DecimalFormat(pattern3,DecimalFormatSymbols.getInstance(Locale.US));
    private static String urlImageAnulado;
    
    
    public static void generar(String path_font, String[] contenido) throws IOException{
        PDDocument document = new PDDocument();
        PDPage page0 = new PDPage(PDRectangle.LETTER);
        document.addPage(page0);
        

        PDPageContentStream contentStream = new PDPageContentStream(document, page0, PDPageContentStream.AppendMode.APPEND, true);
        PDType0Font font = PDType0Font.load(document, new File(path_font));

        contentStream.setFont(font, 12);
        
        float y = page0.getMediaBox().getHeight();
        float ancho = page0.getMediaBox().getWidth();
        y-=20;
        
        String txt, txt_aux;
        for (int i = 0; i < contenido.length; i++) {
            y-=20;
            txt = contenido[i];
                       
            while((txt.length()*6)>ancho){
                write(25, y, txt.substring(0, ((int)ancho/6)));
                txt = txt.substring(((int)ancho/6), txt.length());
                y-=12;
            }            
            write(25, y, txt);
        }
        y-=12;
        
        PDImageXObject pdImage = null;
        try{
            pdImage = PDImageXObject.createFromFile("http://localhost:8080/imagesAdmin/6340999", document);
        }catch(Exception e){
            System.out.println("Imagen registrada");
        }
        
        contentStream.drawImage(pdImage, 25, y, 100, 100);
        contentStream.close();

        document.save("factura.pdf");
        document.close();
    }
    
    public static Color getColorHex(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }
            
    public static void generarFactura( JSONObject contenido, String nombre_, String color) throws IOException, JSONException{
        init();        
        setFontSize(8);
        
        espacios =new int[8];
        espacios[0]=20;
        espacios[1]=90;
        espacios[2]=150;
        espacios[3]=230;
        espacios[4]=410;
        espacios[5]=470;
        espacios[6]=530;
        espacios[7]=592;
        
        JSONObject cabeza = contenido.getJSONObject("CABEZA");
        JSONArray cuerpo = contenido.getJSONArray("CUERPO");
        JSONObject casa = contenido.getJSONObject("CASA");
        
        PDImageXObject pdImage = null;
        
        float y = page.getMediaBox().getHeight();
        float ancho = page.getMediaBox().getWidth();
        y-=60;
        String rs = cabeza.getString("RAZONSOCIALEMISOR");
        String descripcion = cabeza.getString("DESCRIPCION");
        String telefono = cabeza.getString("TELEFONO");
        int nit = cabeza.getInt("NITEMISOR");
        int numero_fact = cabeza.getInt("NUMEROFACTURA");
        int codigo = cabeza.getInt("CODIGOSUCURSAL");
        int codigo_ambiente = cabeza.getInt("CODIGO_AMBIENTE");
        
        String fecha = cabeza.getString("FECHAEMISION");
        String municipio = cabeza.getString("MUNICIPIO");
        String nombre = cabeza.getString("NOMBRERAZONSOCIAL");
        String codigo_cliente = cabeza.getString("CODIGOCLIENTE");
        String numero_documento = cabeza.getString("NUMERODOCUMENTO");
        String cuf = cabeza.getString("CUF");
        String acronimo = cabeza.getString("ACRONIMO");
        double tipo_cambio = cabeza.getDouble("TIPO_DE_CAMBIO_VENTA");
        String leyenda = cabeza.getString("LEYENDA");
        String estado = cabeza.getString("ESTADO");
        String casa_descripcion="N/A";
        String casa_telefono="N/A";
        
        double monto_total=0.0;
        double descuento=0.0;
        double monto_subTotal=0.0;
        double monto_IBCF=0.0;
        double gifCard=0.0;
        
        URL url_ = new URL("http://localhost:8080/perfilEmpresa/"+nit);
        //URL url_ = new URL("http://localhost:8080/perfilEmpresa/"+nit);
        try{
            BufferedImage image_ = ImageIO.read(url_);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image_, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            pdImage = PDImageXObject.createFromByteArray(document, bytes, "prueba");
        }catch(Exception e){
            System.out.println("Imagen registrada");
        }
        
        contentStream.drawImage(pdImage, 25, 720, 170, 40);
        
        
        if(!cabeza.isNull("MONTOTOTAL")){
            monto_total=cabeza.getDouble("MONTOTOTAL");
        }
        if(!cabeza.isNull("DESCUENTOADICIONAL")){
            descuento=cabeza.getDouble("DESCUENTOADICIONAL");
        }
        if(!cabeza.isNull("MONTOTOTALSUJETOIVA")){
            monto_IBCF=cabeza.getDouble("MONTOTOTALSUJETOIVA");
        }
        if(!cabeza.isNull("MONTOGIFCARD")){
            gifCard=cabeza.getDouble("MONTOGIFCARD");
        }
        if(!casa.isNull("DIRECCION")){
            casa_descripcion=casa.getString("DIRECCION");
        }
        if(!casa.isNull("TELEFONO")){
            casa_telefono=casa.getString("TELEFONO");
        }
        String complemento = "";
        
        if(!cabeza.isNull("COMPLEMENTO")){
            complemento = cabeza.getString("COMPLEMENTO");
        }
        
        setBold();
        ArrayList<String> direccion=limitText(cabeza.getString("DIRECCION"),30);
        ArrayList<String> list_casa_direccion=limitText(casa_descripcion,30);
        
        write(330, y, "NIT :");
        write(330, y-9, "N° FACTURA :");
        write(330, y-18, "CODIGO DE");
        write(330, y-27, "AUTORIZACION :");
        setNormal();

        write(420, y, nit+""); 
       write(420, y-9, numero_fact+"");
        ArrayList<String> array_cuf=truncarText(cuf,30);
        float pos_cuf=y;
        for(int j=0;j<array_cuf.size();j++){
            write(420, pos_cuf-18, array_cuf.get(j));
            pos_cuf-=9;
        }
        contentStream.setNonStrokingColor(Color.BLACK); 
        contentStream.setLineWidth(1f);
         
        contentStream.moveTo(320, y+15);
        contentStream.lineTo(espacios[7]-10, y+15);

        contentStream.moveTo(320, pos_cuf-15);
        contentStream.lineTo(espacios[7]-10, pos_cuf-15);

        contentStream.moveTo(320, y + 15);
        contentStream.lineTo(320, pos_cuf-15);

        contentStream.moveTo(espacios[7]-10, y + 15);
        contentStream.lineTo(espacios[7]-10, pos_cuf-15);
        contentStream.stroke(); 
        
        y-=30;
        //setFontSize(6);
        if(codigo==0){
            setBold();
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(textCenter("CASA MATRIZ", 120), y, "CASA MATRIZ");
            y -= 8;
            setNormal();
            write(textCenter(telefono, 120), y, telefono);
            y -= 8;
            for (int j = 0; j < list_casa_direccion.size(); j++) {
                write(textCenter(list_casa_direccion.get(j), 120), y, list_casa_direccion.get(j));
                y -= 8;
            }
        }else{
            setBold();
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(textCenter("SUCURSAL :"+codigo, 120), y, "SUCURSAL :"+codigo);
            y -= 8;
            setNormal();
            write(textCenter(casa_telefono, 120), y, casa_telefono);
            y -= 8;
            for (int j = 0; j < direccion.size(); j++) {
                write(textCenter(direccion.get(j), 120), y, direccion.get(j));
                y -= 8;
            }
            y -= 7;
            setBold();
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(textCenter("CASA MATRIZ", 120), y, "CASA MATRIZ");
            y -= 8;
            setNormal();
            write(textCenter(telefono, 120), y, telefono);
            y -= 8;
            for (int j = 0; j < list_casa_direccion.size(); j++) {
                write(textCenter(list_casa_direccion.get(j), 120), y, list_casa_direccion.get(j));
                y -= 8;
            }
            
        }
      
        
        y-=50;
        setFontSize(10);
        setBold();
        String titulo_factura = "FACTURA";
        String sub_titulo_factura = "(Con Derecho a Credito Fiscal)";
       
                
        write(getTextCenterX(titulo_factura), y, titulo_factura);
        y-=9;
        write(getTextCenterX(sub_titulo_factura), y, sub_titulo_factura);
        y-=20;
        setFontSize(8);
                
        write(espacios[0], y, "Lugar y Fecha:");
        setNormal();
        write(115, y, municipio.toUpperCase()+ ","+getFechaLiteral(fecha));
        setBold();
        write(espacios[4], y, "NIT/CI/CEX :");
        setNormal();
        if(complemento.length()==0){
            write(espacios[4]+50, y, numero_documento);
        }else{
            write(espacios[4]+50, y, numero_documento+complemento);
        }
        
        
        y-=10;
        setBold();
        write(espacios[0], y, "Nombre/Razon Social:");
        setNormal();
        write(115, y, nombre);
        y-=10;
        setBold();
        write(espacios[0], y, "Codigo Cliente:");
        setNormal();
        write(115, y, codigo_cliente);
        
        y-=40;
        ///136, 214, 165
        
        if(color == null) color="#ffffff";
        
        contentStream.setNonStrokingColor(getColorHex(color)); 
        contentStream.addRect(20, y, 572, 25);
        contentStream.fill();  
        
        contentStream.setNonStrokingColor(Color.BLACK); 
        
        contentStream.moveTo(20, y); 
        contentStream.lineTo(592, y);
        
        contentStream.moveTo(20, y+25); 
        contentStream.lineTo(592, y+25); 
        
        contentStream.moveTo(espacios[0], y+25); 
        contentStream.lineTo(espacios[0], y); 
        setBold();
        write(textCenter("CODIGO",PositionMid(espacios[0],espacios[1])), y+13, "CODIGO");
        write(textCenter("PRODUCTO",PositionMid(espacios[0],espacios[1])), y+5, "PRODUCTO");
        
        contentStream.moveTo(espacios[1], y+25); 
        contentStream.lineTo(espacios[1], y); 
        
        write(textCenter("CANTIDAD",PositionMid(espacios[1],espacios[2])), y+10, "CANTIDAD");
        
        contentStream.moveTo(espacios[2], y+25); 
        contentStream.lineTo(espacios[2], y);
        
        write(textCenter("UNIDAD DE",PositionMid(espacios[2],espacios[3])), y+13, "UNIDAD DE");
        write(textCenter("MEDIDA",PositionMid(espacios[2],espacios[3])), y+5, "MEDIDA");
        
        contentStream.moveTo(espacios[3], y+25); 
        contentStream.lineTo(espacios[3], y);
        
        write(textCenter("DESCRIPCION",PositionMid(espacios[3],espacios[4])), y+10, "DESCRIPCION");
        
        
        contentStream.moveTo(espacios[4], y+25); 
        contentStream.lineTo(espacios[4], y);
        
        write(textCenter("PRECIO",PositionMid(espacios[4],espacios[5])), y+13, "PRECIO");
        write(textCenter("UNITARIO",PositionMid(espacios[4],espacios[5])), y+5, "UNITARIO");
        
        contentStream.moveTo(espacios[5], y+25); 
        contentStream.lineTo(espacios[5], y);
        
        write(textCenter("DESCUENTO",PositionMid(espacios[5],espacios[6])), y+10, "DESCUENTO");
        
        contentStream.moveTo(espacios[6], y+25); 
        contentStream.lineTo(espacios[6], y);
        
        write(textCenter("SUBTOTAL",PositionMid(espacios[6],espacios[7])), y+10, "SUBTOTAL");
        
        contentStream.moveTo(espacios[7], y+25); 
        contentStream.lineTo(espacios[7], y);
        
        
        y-=10;
        setFontSize(6);
        setNormal();
        for(int i=0;i<cuerpo.length();i++){
            if (estado.equals("anulada")) {
                PDImageXObject pdImage_ = PDImageXObject.createFromFile(getUrlImageAnulado(),document);
                contentStream.drawImage(pdImage_, getCenterX(190),getCenterY(60)); 
            }
            double monto_descuento =0.0;
            if(!cuerpo.getJSONObject(i).isNull("MONTODESCUENTO")){
                monto_descuento = cuerpo.getJSONObject(i).getDouble("MONTODESCUENTO");
            }
            
            int codigo_producto = cuerpo.getJSONObject(i).getInt("CODIGOPRODUCTO");
            double cantidad = cuerpo.getJSONObject(i).getDouble("CANTIDAD");
            double precio_unitario = cuerpo.getJSONObject(i).getDouble("PRECIOUNITARIO");
            double subtotal = cuerpo.getJSONObject(i).getDouble("SUBTOTAL");
            String descrip = cuerpo.getJSONObject(i).getString("DESCRIPCION");
            String descrip_medida = cuerpo.getJSONObject(i).getString("DESCRIP_MEDIDA");
            
            monto_subTotal+=subtotal;
            
            ArrayList<String>list_descrip=limitText(descrip,30);
            ArrayList<String>list_descrip_medida=limitText(descrip_medida,25);
            
            write(textRight(codigo_producto+"",espacios[1]-2), y, codigo_producto+"");
            write(textRight(getNumFormat(cantidad)+"",espacios[2]-2), y, getNumFormat(cantidad)+"");
            float aux_pos=y;
            for(int j=0;j<list_descrip_medida.size();j++){
                write(espacios[2]+2, aux_pos, list_descrip_medida.get(j));
                aux_pos-=7;
            }
            aux_pos=y;
            for(int j=0;j<list_descrip.size();j++){
                write(espacios[3]+2, aux_pos, list_descrip.get(j));
                aux_pos-=7;
            }
            
            write(textRight(getNumFormat(precio_unitario),espacios[5]-2), y, getNumFormat(precio_unitario));
            write(textRight(getNumFormat(monto_descuento),espacios[6]-2), y, getNumFormat(monto_descuento));
            write(textRight(getNumFormat(subtotal),espacios[7]-2), y,  getNumFormat(subtotal));
            
            float tamano_espacios=list_descrip.size();
            if(tamano_espacios<list_descrip_medida.size()){
                tamano_espacios=list_descrip_medida.size();
            }
            tamano_espacios=y-(tamano_espacios*7);
            
            
            contentStream.moveTo(espacios[0], y+10); 
            contentStream.lineTo(espacios[0], tamano_espacios);
            
            contentStream.moveTo(espacios[1], y+10); 
            contentStream.lineTo(espacios[1], tamano_espacios);
            
            contentStream.moveTo(espacios[2], y+10); 
            contentStream.lineTo(espacios[2], tamano_espacios);
            
            contentStream.moveTo(espacios[3], y+10); 
            contentStream.lineTo(espacios[3], tamano_espacios);
            
            contentStream.moveTo(espacios[4], y+10); 
            contentStream.lineTo(espacios[4], tamano_espacios);
            
            contentStream.moveTo(espacios[5], y+10); 
            contentStream.lineTo(espacios[5], tamano_espacios);
            
            contentStream.moveTo(espacios[6], y+10); 
            contentStream.lineTo(espacios[6], tamano_espacios);
            
            contentStream.moveTo(espacios[7], y+10); 
            contentStream.lineTo(espacios[7], tamano_espacios);
            
            contentStream.moveTo(espacios[0], tamano_espacios); 
            contentStream.lineTo(espacios[7], tamano_espacios);
            
            
            y=y-(y-tamano_espacios);
            y-=10;
            if(y<=150){
                newPage();
                y = page.getMediaBox().getHeight()-50;
            }
            
        }
        y+=10;
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("SUBTOTAL Bs.",espacios[6]-2), y-10, "SUBTOTAL Bs.");
        write(textRight(getNumFormat(monto_subTotal),espacios[7]-2), y-10, getNumFormat(monto_subTotal));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);
        
        
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("(-)DESCUENTO Bs.",espacios[6]-2), y-10, "(-)DESCUENTO Bs.");
        write(textRight(getNumFormat(descuento),espacios[7]-2), y-10, getNumFormat(descuento));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        setBold();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("TOTAL Bs.",espacios[6]-2), y-10, "TOTAL Bs.");
        write(textRight(getNumFormat(monto_subTotal-descuento),espacios[7]-2), y-10, getNumFormat(monto_subTotal-descuento));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        
        setNormal();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("GIFCARD Bs.",espacios[6]-2), y-10, "GIFCARD Bs.");
        write(textRight(getNumFormat(gifCard),espacios[7]-2), y-10, getNumFormat(gifCard));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        setBold();
        write(textRight("MONTO A PAGAR Bs.",espacios[6]-2), y-10, "MONTO A PAGAR Bs.");
        write(textRight(getNumFormat(monto_total),espacios[7]-2), y-10,getNumFormat(monto_total) );
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);
        
        DecimalFormat myFormatter = new DecimalFormat("#");
        myFormatter.setMaximumFractionDigits(2);
        
        
        String literal_num = numero_literal.Convertir(myFormatter.format(monto_total), true);
        
        ArrayList<String> lista_literal = limitText(literal_num, 60);
        write(espacios[0], y-16, "SON :");
        float pos_num = y;
        for(int i=0;i<lista_literal.size();i++){
            write(espacios[0]+50, pos_num-16, lista_literal.get(i));
            pos_num-=8;
        }
        
        setNormal();
        write(espacios[0], pos_num-32, "TIPO CAMBIO :");
        write(espacios[0]+50, pos_num-32, tipo_cambio+" por "+acronimo+" 1.00");
        write(espacios[0], pos_num-40, "TOTAL :");
        write(espacios[0]+50, pos_num-40,getNumFormat((monto_total/tipo_cambio))+" "+acronimo);
        
        
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        setBold();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("IMPORTE BASE CREDITO FISCAL Bs.",espacios[6]-2), y-10, "IMPORTE BASE CREDITO FISCAL Bs.");
        write(textRight(getNumFormat(monto_IBCF),espacios[7]-2), y-10, getNumFormat(monto_IBCF));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=50;
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        setNormal();
        setFontSize(7);
        String mensaje = "ESTA FACTURA CONTRIBUYE AL DESARROLLO DEL PAÍS, EL USO ILÍCITO SERÁ SANCIONADO PENALMENTE DE ACUERDO A LEY";
        write(textCenter(mensaje,235), y, mensaje);
        y-=10;
        ArrayList<String> list_leyenda=limitText(leyenda,80);
        for (int i = 0; i < list_leyenda.size(); i++) {
            write(textCenter(list_leyenda.get(i), 250), y, list_leyenda.get(i));
            y -= 10;
        }
        y-=10;
        
        String documento="'Este Documento es la Representación Gráfica de un Documento Fiscal Digital emitido en una modalidad de facturacion en linea.'";
        String enlace = "www.impuestos.gob.bo";
         y-=10;
        if (estado.equals("enviado")) {
            documento = "Este documento es la Representación Gráfica de un Documento Fiscal Digital emitido fuera de linea, ";
            write(textCenter(documento, 250), y, documento);
            y-=10;
            write(textCenter("verifique su envio con su proveedor o en la página web "+enlace,250), y, "verifique su envio con su proveedor o en la página web "+enlace);
            contentStream.moveTo(300, y-3);
            contentStream.lineTo(373, y-3);
        } else {
            write(textCenter(documento, 250), y, documento);
        }
        
        y-=60;
        final byte[] image;
        String url =  "https://siat.impuestos.gob.bo/consulta/QR?nit=" + nit + "&cuf=" + cuf + "&numero=" + numero_fact + "&t=1";
        try {
            if (codigo_ambiente == 2) {
                url = "https://pilotosiat.impuestos.gob.bo/consulta/QR?nit=" + nit + "&cuf=" + cuf + "&numero=" + numero_fact + "&t=1";
            }
            image = getQRCodeImage(url, 150, 150);
            ByteArrayInputStream bais = new ByteArrayInputStream(image);
            BufferedImage bim = ImageIO.read(bais);

            PDImageXObject pdImage2 = LosslessFactory.createFromImage(document, bim);
            contentStream.drawImage(pdImage2, 460, y);
        } catch (WriterException ex) {
            Logger.getLogger(PDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        
        document.save(nombre_);
        document.close();
    }

    public static void generarFacturaV3( JSONObject contenido, String nombre_, String color) throws IOException, JSONException{
        init();        
        setFontSize(8);
        
        espacios =new int[8];
        espacios[0]=20;
        espacios[1]=90;
        espacios[2]=150;
        espacios[3]=230;
        espacios[4]=410;
        espacios[5]=470;
        espacios[6]=530;
        espacios[7]=592;
        
        JSONObject cabeza = contenido.getJSONObject("CABEZA");
        JSONArray cuerpo = contenido.getJSONArray("CUERPO");
        JSONObject casa = contenido.getJSONObject("CASA");
        
        PDImageXObject pdImage = null;
        
        float y = page.getMediaBox().getHeight();
        float ancho = page.getMediaBox().getWidth();
        y-=60;
        String rs = cabeza.getString("RAZONSOCIALEMISOR");
        String descripcion = cabeza.getString("DESCRIPCION");
        String telefono = cabeza.getString("TELEFONO");
        int nit = cabeza.getInt("NITEMISOR");
        int numero_fact = cabeza.getInt("NUMEROFACTURA");
        int codigo = cabeza.getInt("CODIGOSUCURSAL");
        int codigo_ambiente = cabeza.getInt("CODIGO_AMBIENTE");
        
        String fecha = cabeza.getString("FECHAEMISION");
        String municipio = cabeza.getString("MUNICIPIO");
        String nombre = cabeza.getString("NOMBRERAZONSOCIAL");
        String codigo_cliente = cabeza.getString("CODIGOCLIENTE");
        String numero_documento = cabeza.getString("NUMERODOCUMENTO");
        String cuf = cabeza.getString("CUF");
        String acronimo = cabeza.getString("ACRONIMO");
        double tipo_cambio = cabeza.getDouble("TIPO_DE_CAMBIO_VENTA");
        String leyenda = cabeza.getString("LEYENDA");
        String estado = cabeza.getString("ESTADO");
        String casa_descripcion="N/A";
        String casa_telefono="N/A";
        
        double monto_total=0.0;
        double descuento=0.0;
        double monto_subTotal=0.0;
        double monto_IBCF=0.0;
        double gifCard=0.0;
        
        URL url_ = new URL("http://localhost:8080/perfilEmpresa/"+nit);
        //URL url_ = new URL("http://localhost:8080/perfilEmpresa/"+nit);
        try{
            BufferedImage image_ = ImageIO.read(url_);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image_, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            pdImage = PDImageXObject.createFromByteArray(document, bytes, "prueba");
        }catch(Exception e){
            System.out.println("Imagen registrada");
        }
        
        contentStream.drawImage(pdImage, 25, 720, 170, 40);
        
        
        if(!cabeza.isNull("MONTOTOTAL")){
            monto_total=cabeza.getDouble("MONTOTOTAL");
        }
        if(!cabeza.isNull("DESCUENTOADICIONAL")){
            descuento=cabeza.getDouble("DESCUENTOADICIONAL");
        }
        if(!cabeza.isNull("MONTOTOTALSUJETOIVA")){
            monto_IBCF=cabeza.getDouble("MONTOTOTALSUJETOIVA");
        }
        if(!cabeza.isNull("MONTOGIFCARD")){
            gifCard=cabeza.getDouble("MONTOGIFCARD");
        }
        if(!casa.isNull("DIRECCION")){
            casa_descripcion=casa.getString("DIRECCION");
        }
        if(!casa.isNull("TELEFONO")){
            casa_telefono=casa.getString("TELEFONO");
        }
        String complemento = "";
        
        if(!cabeza.isNull("COMPLEMENTO")){
            complemento = cabeza.getString("COMPLEMENTO");
        }
        
        setBold();
        ArrayList<String> direccion=limitText(cabeza.getString("DIRECCION"),30);
        ArrayList<String> list_casa_direccion=limitText(casa_descripcion,30);
        
        write(330, y, "NIT :");
        write(330, y-9, "N° FACTURA :");
        write(330, y-18, "CODIGO DE");
        write(330, y-27, "AUTORIZACION :");
        setNormal();

        write(420, y, nit+""); 
       write(420, y-9, numero_fact+"");
        ArrayList<String> array_cuf=truncarText(cuf,30);
        float pos_cuf=y;
        for(int j=0;j<array_cuf.size();j++){
            write(420, pos_cuf-18, array_cuf.get(j));
            pos_cuf-=9;
        }
        contentStream.setNonStrokingColor(Color.BLACK); 
        contentStream.setLineWidth(1f);
         
        contentStream.moveTo(320, y+15);
        contentStream.lineTo(espacios[7]-10, y+15);

        contentStream.moveTo(320, pos_cuf-15);
        contentStream.lineTo(espacios[7]-10, pos_cuf-15);

        contentStream.moveTo(320, y + 15);
        contentStream.lineTo(320, pos_cuf-15);

        contentStream.moveTo(espacios[7]-10, y + 15);
        contentStream.lineTo(espacios[7]-10, pos_cuf-15);
        contentStream.stroke(); 
        
        y-=30;
        //setFontSize(6);
        if(codigo==0){
            setBold();
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(textCenter("CASA MATRIZ", 120), y, "CASA MATRIZ");
            y -= 8;
            setNormal();
            write(textCenter(telefono, 120), y, telefono);
            y -= 8;
            
            for (int j = 0; j < list_casa_direccion.size(); j++) {
                write(textCenter(list_casa_direccion.get(j), 120), y, list_casa_direccion.get(j));
                y -= 8;
            }
        }else{
            setBold();
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(textCenter("SUCURSAL :"+codigo, 120), y, "SUCURSAL :"+codigo);
            y -= 8;
            setNormal();
            write(textCenter(casa_telefono, 120), y, casa_telefono);
            y -= 8;
            
            for (int j = 0; j < direccion.size(); j++) {
                write(textCenter(direccion.get(j), 120), y, direccion.get(j));
                y -= 8;
            }
            
        }
      
        
        y-=50;
        setFontSize(10);
        setBold();
        String titulo_factura = "FACTURA";
        String sub_titulo_factura = "(Con Derecho a Credito Fiscal)";
       
                
        write(getTextCenterX(titulo_factura), y, titulo_factura);
        y-=9;
        write(getTextCenterX(sub_titulo_factura), y, sub_titulo_factura);
        y-=20;
        setFontSize(8);
                
        write(espacios[0], y, "Lugar y Fecha:");
        setNormal();
        write(115, y, municipio.toUpperCase()+ ","+getFechaLiteral(fecha));
        setBold();
        write(espacios[4], y, "NIT/CI/CEX :");
        setNormal();
        if(complemento.length()==0){
            write(espacios[4]+50, y, numero_documento);
        }else{
            write(espacios[4]+50, y, numero_documento+complemento);
        }
        
        
        y-=10;
        setBold();
        write(espacios[0], y, "Nombre/Razon Social:");
        setNormal();
        write(115, y, nombre);
        y-=10;
        setBold();
        write(espacios[0], y, "Codigo Cliente:");
        setNormal();
        write(115, y, codigo_cliente);
        
        y-=40;
        ///136, 214, 165
        
        if(color == null) color="#ffffff";
        
        contentStream.setNonStrokingColor(getColorHex(color)); 
        contentStream.addRect(20, y, 572, 25);
        contentStream.fill();  
        
        contentStream.setNonStrokingColor(Color.BLACK); 
        
        contentStream.moveTo(20, y); 
        contentStream.lineTo(592, y);
        
        contentStream.moveTo(20, y+25); 
        contentStream.lineTo(592, y+25); 
        
        contentStream.moveTo(espacios[0], y+25); 
        contentStream.lineTo(espacios[0], y); 
        setBold();
        write(textCenter("CODIGO",PositionMid(espacios[0],espacios[1])), y+13, "CODIGO");
        write(textCenter("PRODUCTO",PositionMid(espacios[0],espacios[1])), y+5, "PRODUCTO");
        
        contentStream.moveTo(espacios[1], y+25); 
        contentStream.lineTo(espacios[1], y); 
        
        write(textCenter("CANTIDAD",PositionMid(espacios[1],espacios[2])), y+10, "CANTIDAD");
        
        contentStream.moveTo(espacios[2], y+25); 
        contentStream.lineTo(espacios[2], y);
        
        write(textCenter("UNIDAD DE",PositionMid(espacios[2],espacios[3])), y+13, "UNIDAD DE");
        write(textCenter("MEDIDA",PositionMid(espacios[2],espacios[3])), y+5, "MEDIDA");
        
        contentStream.moveTo(espacios[3], y+25); 
        contentStream.lineTo(espacios[3], y);
        
        write(textCenter("DESCRIPCION",PositionMid(espacios[3],espacios[4])), y+10, "DESCRIPCION");
        
        
        contentStream.moveTo(espacios[4], y+25); 
        contentStream.lineTo(espacios[4], y);
        
        write(textCenter("PRECIO",PositionMid(espacios[4],espacios[5])), y+13, "PRECIO");
        write(textCenter("UNITARIO",PositionMid(espacios[4],espacios[5])), y+5, "UNITARIO");
        
        contentStream.moveTo(espacios[5], y+25); 
        contentStream.lineTo(espacios[5], y);
        
        write(textCenter("DESCUENTO",PositionMid(espacios[5],espacios[6])), y+10, "DESCUENTO");
        
        contentStream.moveTo(espacios[6], y+25); 
        contentStream.lineTo(espacios[6], y);
        
        write(textCenter("SUBTOTAL",PositionMid(espacios[6],espacios[7])), y+10, "SUBTOTAL");
        
        contentStream.moveTo(espacios[7], y+25); 
        contentStream.lineTo(espacios[7], y);
        
        
        y-=10;
        setFontSize(6);
        setNormal();
        for(int i=0;i<cuerpo.length();i++){
            if (estado.equals("anulada")) {
                PDImageXObject pdImage_ = PDImageXObject.createFromFile(getUrlImageAnulado(),document);
                contentStream.drawImage(pdImage_, getCenterX(190),getCenterY(60)); 
            }
            double monto_descuento =0.0;
            if(!cuerpo.getJSONObject(i).isNull("MONTODESCUENTO")){
                monto_descuento = cuerpo.getJSONObject(i).getDouble("MONTODESCUENTO");
            }
            
            int codigo_producto = cuerpo.getJSONObject(i).getInt("CODIGOPRODUCTO");
            double cantidad = cuerpo.getJSONObject(i).getDouble("CANTIDAD");
            double precio_unitario = cuerpo.getJSONObject(i).getDouble("PRECIOUNITARIO");
            double subtotal = cuerpo.getJSONObject(i).getDouble("SUBTOTAL");
            String descrip = cuerpo.getJSONObject(i).getString("DESCRIPCION");
            String descrip_medida = cuerpo.getJSONObject(i).getString("DESCRIP_MEDIDA");
            
            monto_subTotal+=subtotal;
            
            ArrayList<String>list_descrip=limitText(descrip,30);
            ArrayList<String>list_descrip_medida=limitText(descrip_medida,25);
            
            write(textRight(codigo_producto+"",espacios[1]-2), y, codigo_producto+"");
            write(textRight(getNumFormat(cantidad)+"",espacios[2]-2), y, getNumFormat(cantidad)+"");
            float aux_pos=y;
            for(int j=0;j<list_descrip_medida.size();j++){
                write(espacios[2]+2, aux_pos, list_descrip_medida.get(j));
                aux_pos-=7;
            }
            aux_pos=y;
            for(int j=0;j<list_descrip.size();j++){
                write(espacios[3]+2, aux_pos, list_descrip.get(j));
                aux_pos-=7;
            }
            
            write(textRight(getNumFormat(precio_unitario),espacios[5]-2), y, getNumFormat(precio_unitario));
            write(textRight(getNumFormat(monto_descuento),espacios[6]-2), y, getNumFormat(monto_descuento));
            write(textRight(getNumFormat(subtotal),espacios[7]-2), y,  getNumFormat(subtotal));
            
            float tamano_espacios=list_descrip.size();
            if(tamano_espacios<list_descrip_medida.size()){
                tamano_espacios=list_descrip_medida.size();
            }
            tamano_espacios=y-(tamano_espacios*7);
            
            
            contentStream.moveTo(espacios[0], y+10); 
            contentStream.lineTo(espacios[0], tamano_espacios);
            
            contentStream.moveTo(espacios[1], y+10); 
            contentStream.lineTo(espacios[1], tamano_espacios);
            
            contentStream.moveTo(espacios[2], y+10); 
            contentStream.lineTo(espacios[2], tamano_espacios);
            
            contentStream.moveTo(espacios[3], y+10); 
            contentStream.lineTo(espacios[3], tamano_espacios);
            
            contentStream.moveTo(espacios[4], y+10); 
            contentStream.lineTo(espacios[4], tamano_espacios);
            
            contentStream.moveTo(espacios[5], y+10); 
            contentStream.lineTo(espacios[5], tamano_espacios);
            
            contentStream.moveTo(espacios[6], y+10); 
            contentStream.lineTo(espacios[6], tamano_espacios);
            
            contentStream.moveTo(espacios[7], y+10); 
            contentStream.lineTo(espacios[7], tamano_espacios);
            
            contentStream.moveTo(espacios[0], tamano_espacios); 
            contentStream.lineTo(espacios[7], tamano_espacios);
            
            
            y=y-(y-tamano_espacios);
            y-=10;
            if(y<=150){
                newPage();
                y = page.getMediaBox().getHeight()-50;
            }
            
        }
        y+=10;
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("SUBTOTAL Bs.",espacios[6]-2), y-10, "SUBTOTAL Bs.");
        write(textRight(getNumFormat(monto_subTotal),espacios[7]-2), y-10, getNumFormat(monto_subTotal));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);
        
        
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("(-)DESCUENTO Bs.",espacios[6]-2), y-10, "(-)DESCUENTO Bs.");
        write(textRight(getNumFormat(descuento),espacios[7]-2), y-10, getNumFormat(descuento));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        setBold();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("TOTAL Bs.",espacios[6]-2), y-10, "TOTAL Bs.");
        write(textRight(getNumFormat(monto_subTotal-descuento),espacios[7]-2), y-10, getNumFormat(monto_subTotal-descuento));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        
        setNormal();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("GIFCARD Bs.",espacios[6]-2), y-10, "GIFCARD Bs.");
        write(textRight(getNumFormat(gifCard),espacios[7]-2), y-10, getNumFormat(gifCard));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        setBold();
        write(textRight("MONTO A PAGAR Bs.",espacios[6]-2), y-10, "MONTO A PAGAR Bs.");
        write(textRight(getNumFormat(monto_total),espacios[7]-2), y-10,getNumFormat(monto_total) );
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);
        
        DecimalFormat myFormatter = new DecimalFormat("#");
        myFormatter.setMaximumFractionDigits(2);
        
        
        String literal_num = numero_literal.Convertir(myFormatter.format(monto_total), true);
        
        ArrayList<String> lista_literal = limitText(literal_num, 60);
        write(espacios[0], y-16, "SON :");
        float pos_num = y;
        for(int i=0;i<lista_literal.size();i++){
            write(espacios[0]+50, pos_num-16, lista_literal.get(i));
            pos_num-=8;
        }
        
        setNormal();
        write(espacios[0], pos_num-32, "TIPO CAMBIO :");
        write(espacios[0]+50, pos_num-32, tipo_cambio+" por "+acronimo+" 1.00");
        write(espacios[0], pos_num-40, "TOTAL :");
        write(espacios[0]+50, pos_num-40,getNumFormat((monto_total/tipo_cambio))+" "+acronimo);
        
        
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        setBold();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("IMPORTE BASE CREDITO FISCAL Bs.",espacios[6]-2), y-10, "IMPORTE BASE CREDITO FISCAL Bs.");
        write(textRight(getNumFormat(monto_IBCF),espacios[7]-2), y-10, getNumFormat(monto_IBCF));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=50;
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        setNormal();
        setFontSize(7);
        String mensaje = "ESTA FACTURA CONTRIBUYE AL DESARROLLO DEL PAÍS, EL USO ILÍCITO SERÁ SANCIONADO PENALMENTE DE ACUERDO A LEY";
        write(textCenter(mensaje,235), y, mensaje);
        y-=10;
        ArrayList<String> list_leyenda=limitText(leyenda,80);
        for (int i = 0; i < list_leyenda.size(); i++) {
            write(textCenter(list_leyenda.get(i), 250), y, list_leyenda.get(i));
            y -= 10;
        }
        y-=10;
        
        String documento="'Este Documento es la Representación Gráfica de un Documento Fiscal Digital emitido en una modalidad de facturacion en linea.'";
        String enlace = "www.impuestos.gob.bo";
         y-=10;
        if (estado.equals("enviado")) {
            documento = "Este documento es la Representación Gráfica de un Documento Fiscal Digital emitido fuera de linea, ";
            write(textCenter(documento, 250), y, documento);
            y-=10;
            write(textCenter("verifique su envio con su proveedor o en la página web "+enlace,250), y, "verifique su envio con su proveedor o en la página web "+enlace);
            contentStream.moveTo(300, y-3);
            contentStream.lineTo(373, y-3);
        } else {
            write(textCenter(documento, 250), y, documento);
        }
        
        y-=60;
        final byte[] image;
        String url =  "https://siat.impuestos.gob.bo/consulta/QR?nit=" + nit + "&cuf=" + cuf + "&numero=" + numero_fact + "&t=1";
        try {
            if (codigo_ambiente == 2) {
                url = "https://pilotosiat.impuestos.gob.bo/consulta/QR?nit=" + nit + "&cuf=" + cuf + "&numero=" + numero_fact + "&t=1";
            }
            image = getQRCodeImage(url, 150, 150);
            ByteArrayInputStream bais = new ByteArrayInputStream(image);
            BufferedImage bim = ImageIO.read(bais);

            PDImageXObject pdImage2 = LosslessFactory.createFromImage(document, bim);
            contentStream.drawImage(pdImage2, 460, y);
        } catch (WriterException ex) {
            Logger.getLogger(PDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        
        document.save(nombre_);
        document.close();
    }

    public static void prueba(String nombre_, String color) throws IOException, JSONException{
        init();        
        setFontSize(8);
        
        espacios =new int[8];
        espacios[0]=20;
        espacios[1]=90;
        espacios[2]=150;
        espacios[3]=230;
        espacios[4]=410;
        espacios[5]=470;
        espacios[6]=530;
        espacios[7]=592;
        
        
        PDImageXObject pdImage = null;
        
        float y = page.getMediaBox().getHeight();
        float ancho = page.getMediaBox().getWidth();
        y-=60;
        
        write(330, y, "NIT :");
        write(330, y-9, "N° FACTURA :");
        write(330, y-18, "CODIGO DE");
        write(330, y-27, "AUTORIZACION :");
        setNormal();

        write(420, y, "el nit"); 
       write(420, y-9, "num fact");
        ArrayList<String> array_cuf=truncarText("xcvxcvxcvd",30);
        float pos_cuf=y;
        for(int j=0;j<array_cuf.size();j++){
            write(420, pos_cuf-18, array_cuf.get(j));
            pos_cuf-=9;
        }
        contentStream.setNonStrokingColor(Color.BLACK); 
        contentStream.setLineWidth(1f);
         
        contentStream.moveTo(320, y+15);
        contentStream.lineTo(espacios[7]-10, y+15);

        contentStream.moveTo(320, pos_cuf-15);
        contentStream.lineTo(espacios[7]-10, pos_cuf-15);

        contentStream.moveTo(320, y + 15);
        contentStream.lineTo(320, pos_cuf-15);

        contentStream.moveTo(espacios[7]-10, y + 15);
        contentStream.lineTo(espacios[7]-10, pos_cuf-15);
        contentStream.stroke(); 
        
        y-=30;
        //setFontSize(6);
        
        if(color == null) color="#ffffff";
        
        contentStream.setNonStrokingColor(getColorHex(color)); 
        contentStream.addRect(20, y, 572, 25);
        contentStream.fill();  
        
        contentStream.setNonStrokingColor(Color.BLACK); 
        
        contentStream.moveTo(20, y); 
        contentStream.lineTo(592, y);
        
        contentStream.moveTo(20, y+25); 
        contentStream.lineTo(592, y+25); 
        
        contentStream.moveTo(espacios[0], y+25); 
        contentStream.lineTo(espacios[0], y); 
        setBold();
        write(textCenter("CODIGO",PositionMid(espacios[0],espacios[1])), y+13, "CODIGO");
        write(textCenter("PRODUCTO",PositionMid(espacios[0],espacios[1])), y+5, "PRODUCTO");
        
        contentStream.moveTo(espacios[1], y+25); 
        contentStream.lineTo(espacios[1], y); 
        
        write(textCenter("CANTIDAD",PositionMid(espacios[1],espacios[2])), y+10, "CANTIDAD");
        
        contentStream.moveTo(espacios[2], y+25); 
        contentStream.lineTo(espacios[2], y);
        
        write(textCenter("UNIDAD DE",PositionMid(espacios[2],espacios[3])), y+13, "UNIDAD DE");
        write(textCenter("MEDIDA",PositionMid(espacios[2],espacios[3])), y+5, "MEDIDA");
        
        contentStream.moveTo(espacios[3], y+25); 
        contentStream.lineTo(espacios[3], y);
        
        write(textCenter("DESCRIPCION",PositionMid(espacios[3],espacios[4])), y+10, "DESCRIPCION");
        
        
        contentStream.moveTo(espacios[4], y+25); 
        contentStream.lineTo(espacios[4], y);
        
        write(textCenter("PRECIO",PositionMid(espacios[4],espacios[5])), y+13, "PRECIO");
        write(textCenter("UNITARIO",PositionMid(espacios[4],espacios[5])), y+5, "UNITARIO");
        
        contentStream.moveTo(espacios[5], y+25); 
        contentStream.lineTo(espacios[5], y);
        
        write(textCenter("DESCUENTO",PositionMid(espacios[5],espacios[6])), y+10, "DESCUENTO");
        
        contentStream.moveTo(espacios[6], y+25); 
        contentStream.lineTo(espacios[6], y);
        
        write(textCenter("SUBTOTAL",PositionMid(espacios[6],espacios[7])), y+10, "SUBTOTAL");
        
        contentStream.moveTo(espacios[7], y+25); 
        contentStream.lineTo(espacios[7], y);
        
       
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
  
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
  
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        
        setNormal();
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        write(textRight("GIFCARD Bs.",espacios[6]-2), y-10, "GIFCARD Bs.");
     //   write(textRight(getNumFormat(gifCard),espacios[7]-2), y-10, getNumFormat(gifCard));
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);

        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        contentStream.moveTo(espacios[4], y);
        contentStream.lineTo(espacios[4], y-20);
        setBold();
        write(textRight("MONTO A PAGAR Bs.",espacios[6]-2), y-10, "MONTO A PAGAR Bs.");
        //write(textRight(getNumFormat(monto_total),espacios[7]-2), y-10,getNumFormat(monto_total) );
        contentStream.moveTo(espacios[6], y);
        contentStream.lineTo(espacios[6], y-20);
        
        DecimalFormat myFormatter = new DecimalFormat("#");
        myFormatter.setMaximumFractionDigits(2);
        
        
        //String literal_num = numero_literal.Convertir(myFormatter.format(monto_total), true);
        
        
        contentStream.moveTo(espacios[7], y);
        contentStream.lineTo(espacios[7], y-20);
        
        contentStream.moveTo(espacios[4], y-20);
        contentStream.lineTo(espacios[7], y-20);
        y-=20;
        if (y <= 150) {
            newPage();
            y = page.getMediaBox().getHeight()-50;
        }
        
        
        
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        
        document.save(nombre_); 
        document.close();
        
    }
    
    public static void generarFacturaRollo( JSONObject contenido, String nombre_, String color) throws IOException, JSONException{
        int alto_cal = getHeight_RolloDPF(contenido);
        initOffSet(alto_cal);        
        setFontSize(8);
        
        espacios =new int[8];
        espacios[0]=20;
        espacios[1]=90;
        espacios[2]=150;
        espacios[3]=230;
        espacios[4]=410;
        espacios[5]=470;
        espacios[6]=530;
        espacios[7]=592;
        
    
        
        JSONObject cabeza = contenido.getJSONObject("CABEZA");
        JSONArray cuerpo = contenido.getJSONArray("CUERPO");
        JSONObject casa = contenido.getJSONObject("CASA");
        
        PDImageXObject pdImage = null;
        
        
        float y = page.getMediaBox().getHeight();
        
        y-=20;
        String rs = cabeza.getString("RAZONSOCIALEMISOR");
        String telefono = cabeza.getString("TELEFONO");
        int nit = cabeza.getInt("NITEMISOR");
        int numero_fact = cabeza.getInt("NUMEROFACTURA");
        int codigo = cabeza.getInt("CODIGOSUCURSAL");
        int codigo_ambiente = cabeza.getInt("CODIGO_AMBIENTE");
        
        String fecha = cabeza.getString("FECHAEMISION");
        String municipio = cabeza.getString("MUNICIPIO");
        String nombre = cabeza.getString("NOMBRERAZONSOCIAL");
        String codigo_cliente = cabeza.getString("CODIGOCLIENTE");
        String numero_documento = cabeza.getString("NUMERODOCUMENTO");
        String cuf = cabeza.getString("CUF");
        String leyenda = cabeza.getString("LEYENDA");
        String estado = cabeza.getString("ESTADO");
        String casa_descripcion="N/A";
        String casa_telefono="N/A";
        
        double monto_total=0.0;
        double descuento=0.0;
        double monto_subTotal=0.0;
        double monto_IBCF=0.0;
        double gifCard=0.0;
        
      /*  URL url_ = new URL("http://localhost:8080/perfilEmpresa/"+nit);
        //URL url_ = new URL("http://localhost:8080/perfilEmpresa/"+nit);
        BufferedImage image_ = ImageIO.read(url_);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image_, "PNG", baos);
        byte[] bytes = baos.toByteArray();
        
        try{
            pdImage = PDImageXObject.createFromByteArray(document, bytes, "prueba");
        }catch(Exception e){
            System.out.println("Imagen registrada");
        }
        contentStream.drawImage(pdImage, 25, 720, 170, 40);*/
        
        
        if(!cabeza.isNull("MONTOTOTAL")){
            monto_total=cabeza.getDouble("MONTOTOTAL");
        }
        if(!cabeza.isNull("DESCUENTOADICIONAL")){
            descuento=cabeza.getDouble("DESCUENTOADICIONAL");
        }

        if(!cabeza.isNull("MONTOTOTALSUJETOIVA")){
            monto_IBCF=cabeza.getDouble("MONTOTOTALSUJETOIVA");
        }
        if(!cabeza.isNull("MONTOGIFCARD")){
            gifCard=cabeza.getDouble("MONTOGIFCARD");
        }
        
        if(!casa.isNull("DIRECCION")){
            casa_descripcion=casa.getString("DIRECCION");
        }
        if(!casa.isNull("TELEFONO")){
            casa_telefono=casa.getString("TELEFONO");
        }
    
        
        String complemento_ = "";
        
        
        if(!cabeza.isNull("COMPLEMENTO")){
            complemento_ = cabeza.get("COMPLEMENTO")+"";
        }
        
        
        ArrayList<String> direccion=limitText(cabeza.getString("DIRECCION"),30);
        ArrayList<String> list_casa_direccion=limitText(casa_descripcion,30);
        
        setFontSize(8);
        String titulo_factura = "FACTURA";
        String sub_titulo_factura = "CON DERECHO A CRÉDITO FISCAL";
        
        setBold();
        write(getTextCenterX(titulo_factura), y, titulo_factura);
        y-=9;
        write(getTextCenterX(sub_titulo_factura), y, sub_titulo_factura);
        y-=9;
        setNormal();
        setFontSize(7);
        if(codigo==0){
            write(getTextCenterX(rs), y, rs);
            y -= 10;
            write(getTextCenterX("CASA MATRIZ"), y, "CASA MATRIZ");
            
            y -= 8;
            for (int j = 0; j < direccion.size(); j++) {
                write(getTextCenterX(direccion.get(j)), y, direccion.get(j));
                y -= 8;
            }
            write(getTextCenterX("Tel. "+telefono), y, "Tel. "+telefono);
        }else{
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(getTextCenterX("CASA MATRIZ"), y, "CASA MATRIZ");
            y -= 8;
            write(getTextCenterX(casa_telefono), y, casa_telefono);
            y -= 8;
            for (int j = 0; j < list_casa_direccion.size(); j++) {
                write(getTextCenterX(list_casa_direccion.get(j)), y, list_casa_direccion.get(j));
                y -= 8;
            }
            write(textCenter(rs, 120), y, rs);
            y -= 10;
            write(getTextCenterX("SUCURSAL :"+codigo), y, "SUCURSAL :"+codigo);
            y -= 8;
            for (int j = 0; j < direccion.size(); j++) {
                write(getTextCenterX(direccion.get(j)), y, direccion.get(j));
                y -= 8;
            }
            y -= 8;
            write(getTextCenterX("Tel. "+telefono), y, "Tel. "+telefono);
        }
        y -= 8;
        write(getTextCenterX(municipio), y,municipio);
        y -= 8;
        setNormal();
        drawLinePoint(y);
        y-=16;
        setBold();
        write(getTextCenterX("NIT"), y, "NIT");
        write(getTextCenterX("N° FACTURA"), y-18, "N° FACTURA");
        write(getTextCenterX("CÓD. AUTORIZACION"), y-36, "COD. AUTORIZACION");
        setNormal();
        
        y=y-9;
        write(getTextCenterX(nit+""), y, nit+"");
        y=y-18;
        write(getTextCenterX(numero_fact+""), y, numero_fact+"");
        y=y-9;
        ArrayList<String> array_cuf=truncarText(cuf,30);
        float pos_cuf=y;
        for(int j=0;j<array_cuf.size();j++){
            write(getTextCenterX(array_cuf.get(j)), pos_cuf-9, array_cuf.get(j));
            pos_cuf-=9;
        }
        y=pos_cuf;
        setNormal();
        y-=9;
        drawLinePoint(y);
        setBold();
        y-=17;
        setBold();
        write(textRight("NOMBRE/RAZÓN SOCIAL: ", (ANCHO/2)-1), y, "NOMBRE/RAZÓN SOCIAL: ");
        setNormal();
        ArrayList<String> list_NOMBRE = limitText(nombre , 15);

        float aux_pos_nombre = y;

        for (int j = 0; j < list_NOMBRE.size(); j++) {
            write((ANCHO/2)+1, aux_pos_nombre, list_NOMBRE.get(j));
            aux_pos_nombre -= 8;
        }
        y = aux_pos_nombre-2;
        setBold();
        write(textRight("NIT/CI/CEX: ", (ANCHO/2)-1), y, "NIT/CI/CEX: ");
        setNormal();
        if(complemento_.length()==0){
            write((ANCHO/2)+1, y, numero_documento);
        }else{
            write((ANCHO/2)+1, y, numero_documento+complemento_);
        }
        y-=10;
        setBold();
        write(textRight("COD. CLIENTE: ", (ANCHO/2)-1), y, "COD. CLIENTE: ");
        setNormal();
        write((ANCHO/2)+1, y, codigo_cliente);
        y-=10;
        setBold();
        write(textRight("FECHA DE EMISIÓN: ", (ANCHO/2)-1), y, "FECHA DE EMISIÓN: ");
        setNormal();
        write((ANCHO/2)+1, y, getFechaFormat(fecha));
        y-=9;
        drawLinePoint(y);
        y-=17;
        setBold();
        write(getTextCenterX("DETALLE"), y, "DETALLE");
        y-=10;
        
        
        setNormal();
        for(int i=0;i<cuerpo.length();i++){
            if (estado.equals("anulada")) {
                PDImageXObject pdImage_ = PDImageXObject.createFromFile(getUrlImageAnulado(),document);
                contentStream.drawImage(pdImage_, getCenterX(190),getCenterY(60)); 
            }
            double monto_descuento =0.0;
            if(!cuerpo.getJSONObject(i).isNull("MONTODESCUENTO")){
                monto_descuento = cuerpo.getJSONObject(i).getDouble("MONTODESCUENTO");
            }
            
            int codigo_producto = cuerpo.getJSONObject(i).getInt("CODIGOPRODUCTO");
            double cantidad = cuerpo.getJSONObject(i).getDouble("CANTIDAD");
            double precio_unitario = cuerpo.getJSONObject(i).getDouble("PRECIOUNITARIO");
            double subtotal = cuerpo.getJSONObject(i).getDouble("SUBTOTAL");
            String descrip = cuerpo.getJSONObject(i).getString("DESCRIPCION");
            String descrip_medida = cuerpo.getJSONObject(i).getString("DESCRIP_MEDIDA");
            
            monto_subTotal+=subtotal;
            
            ArrayList<String>list_descrip=limitText(codigo_producto+"-"+descrip,28);
            
            float aux_pos=y;
            
            for(int j=0;j<list_descrip.size();j++){
                setBold();
                write(20, aux_pos, list_descrip.get(j));
                aux_pos-=8;
            }
            y=aux_pos;
            setNormal();
            write(20, y,  getNumFormat3(cantidad)+" X "+getNumFormat(precio_unitario));
            write(textRight( getNumFormat(precio_unitario), ANCHO-17), y,getNumFormat(subtotal));
            y-=12;
        }
        
       /* write(20, y,  "1.00 X "+getNumFormat(monto_total));
        write(textRight(getNumFormat((monto_total)),ANCHO-17), y,  getNumFormat((monto_total)));
        y-=8;*/
        drawPoints(y, 110);
        y-=10;
        
        write(textRight("SUBTOTAL Bs.",(ANCHO/2)+4), y, "SUBTOTAL Bs.");
        write(textRight(getNumFormat(monto_subTotal),ANCHO-17), y, getNumFormat(monto_subTotal));
        y-=9;
        
        write(textRight("DESCUENTO Bs.",(ANCHO/2)+4), y, "DESCUENTO Bs.");
        write(textRight(getNumFormat(descuento),ANCHO-17), y, getNumFormat(descuento));
        y-=9;
        write(textRight("TOTAL Bs.",(ANCHO/2)+4), y, "TOTAL Bs.");
        write(textRight(getNumFormat((monto_total)),ANCHO-17), y,getNumFormat((monto_total)));
        y-=9;
        
        
        write(textRight("MONTO GIFCARD Bs.",(ANCHO/2)+4), y, "MONTO GIFCARD Bs.");
        write(textRight(getNumFormat(gifCard),ANCHO-17), y, getNumFormat(gifCard));
        y-=9;
        
       
        setBold();
        write(textRight("TOTAL A PAGAR Bs.",(ANCHO/2)+4), y, "TOTAL A PAGAR Bs.");
        write(textRight(getNumFormat(monto_total),ANCHO-17), y,getNumFormat(monto_total) );
        y-=9;
        write(textRight("IMPORTE BASE CREDITO FISCAL",(ANCHO/2)+4), y, "IMPORTE BASE CREDITO FISCAL");
        write(textRight(getNumFormat(monto_IBCF),ANCHO-17), y, getNumFormat(monto_IBCF));
        y-=9;
        DecimalFormat myFormatter = new DecimalFormat("#.00");
        //myFormatter.setMaximumFractionDigits(2);
        
        y-=18;
        String literal_num = numero_literal.Convertir(myFormatter.format(monto_total), true);
        setNormal();
        ArrayList<String> lista_literal = limitText("SON :"+literal_num, 60);
        float pos_num = y;
        for(int i=0;i<lista_literal.size();i++){
            write(15, pos_num, lista_literal.get(i));
            pos_num-=8;
        }
        y=pos_num;
        drawLinePoint(y);
        y-=17;
        
        setNormal();
        setFontSize(7);
        String mensaje = "ESTA FACTURA CONTRIBUYE AL DESARROLLO DEL PAÍS, EL USO ILÍCITO SERÁ SANCIONADO PENALMENTE DE ACUERDO A LEY";
        ArrayList<String> lis_aux = limitText(mensaje, 40);
        float pos_aux = y;
        for(int i=0;i<lis_aux.size();i++){
            write( getTextCenterX(lis_aux.get(i)), pos_aux, lis_aux.get(i));
            pos_aux-=8;
        }
        y=pos_aux;
        y-=10;
        ArrayList<String> list_leyenda=limitText(leyenda,40);
        for (int i = 0; i < list_leyenda.size(); i++) {
            write(getTextCenterX(list_leyenda.get(i)), y, list_leyenda.get(i));
            y -= 10;
        }
        
        String documento="'Este Documento es la Representación Gráfica de un Documento Fiscal Digital emitido en una modalidad de facturacion en linea.'";
        String enlace = "www.impuestos.gob.bo";
         y-=2;
        if (estado.equals("enviado")) {
            documento = "Este documento es la Representación Gráfica de un Documento Fiscal Digital emitido fuera de linea, ";
            lis_aux = limitText(documento, 40);
            pos_aux = y;
            for(int i=0;i<lis_aux.size();i++){
                write( getTextCenterX(lis_aux.get(i)), pos_aux, lis_aux.get(i));
                pos_aux-=8;
            }
            y=pos_aux;
            write(getTextCenterX("verifique su envio con su proveedor o en la página web"), y, "verifique su envio con su proveedor o en la página web");
            y-=8;
            write(getTextCenterX(enlace), y, enlace);
            y-=8;

        } else {            
            lis_aux = limitText(documento, 40);
            pos_aux = y;
            for(int i=0;i<lis_aux.size();i++){
                write( getTextCenterX(lis_aux.get(i)), pos_aux, lis_aux.get(i));
                pos_aux-=8;
            }
            y=pos_aux;
        }
        //y-=150;
        final byte[] image;
        String url =  "https://siat.impuestos.gob.bo/consulta/QR?nit=" + nit + "&cuf=" + cuf + "&numero=" + numero_fact + "&t=1";
        try {
            if (codigo_ambiente == 2) {
                url = "https://pilotosiat.impuestos.gob.bo/consulta/QR?nit=" + nit + "&cuf=" + cuf + "&numero=" + numero_fact + "&t=1";
            }
            int tamano_qr = Math.round(y-10);
            if(tamano_qr >= 150){
                tamano_qr=150;
            }
            
            image = getQRCodeImage(url, tamano_qr, tamano_qr);
            ByteArrayInputStream bais = new ByteArrayInputStream(image);
            BufferedImage bim = ImageIO.read(bais);
            y-=tamano_qr;
            PDImageXObject pdImage2 = LosslessFactory.createFromImage(document, bim);
            contentStream.drawImage(pdImage2, (ANCHO/2)-(tamano_qr/2), y);
            
        } catch (WriterException ex) {
            Logger.getLogger(PDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        document.save(nombre_);
        document.close();
    }
    public static void generarFacturaOffSet(JSONObject option) throws IOException, JSONException{
        //initOffSet();        
        setFontSize(8);
        JSONArray cabezera = option.getJSONArray("head");
        JSONArray cuerpo = option.getJSONArray("body");
    }
    
    public static void generarPDF(JSONObject option) throws IOException, JSONException{
        init();        
        setFontSize(8);
        String color = option.getString("color");
        JSONArray cabezera = option.getJSONArray("head");
        JSONArray cuerpo = option.getJSONArray("body");
        TableHead(cabezera);
        TableBody(cuerpo);
        
    }
    
    public static void write(float x, float y, String text) throws IOException {
        //String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");.
        String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        try {
            contentStream.showText(line);
        } catch (Exception e) {
            // line = new String(text.getBytes("ISO-8859-1"), "UTF-8");
            contentStream.showText(text);
        }
        contentStream.endText();
    }
    public static void TableHead(JSONArray head) throws JSONException{
        float max_ancho=0;
        for(int i=0;i<head.length();i++){
            JSONObject obj = head.getJSONObject(i);
            String tittle = obj.getString("tittle");
            float width =Float.parseFloat(obj.getString("width"));
            
        }
    }
    public static void TableBody(JSONArray cuerpo){
        JSONObject obj = new JSONObject();
    }
    
    public void footer(JSONObject footer){
        try {
            if(footer.getBoolean("user")){
                
            }
        } catch (JSONException ex) {
        }        
    }
    public static float getTextCenterX(String text) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (mediaBox.getWidth() - tamanoFontX) / 2;
        return startX;
    }
    public static float getTextCenterY(String text) throws IOException{
        float tamanoFontY = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        float startY = (mediaBox.getHeight() - tamanoFontY) / 2;
        return startY;
    }
    public static float textCenter(String text,int x) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (x - (tamanoFontX/2));
        return startX;
    }
    public static float textRight(String text,int x) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (x - (tamanoFontX));
        return startX;
    }
    public static float getCenterX(float ancho) throws IOException{
        float startX = (mediaBox.getWidth() - ancho) / 2;
        return startX;
    }
    public static float getCenterY(float alto) throws IOException{
        float startY = (mediaBox.getHeight() - alto) / 2;
        return startY;
    }
    public static void init() throws IOException{
        numero_literal= new NumeroLiteral();
        document = new PDDocument();
        page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        font = PDType0Font.load(document, new File(getRutaFont()));
        contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
        mediaBox = page.getMediaBox();
    }
    public static void initOffSet(int cal) throws IOException{
        ALTO=671;
        ANCHO=241;
        ALTO_MARGEN = 874;
        numero_literal= new NumeroLiteral();
        document = new PDDocument();
        //page = new PDPage(new PDRectangle(ANCHO, cal));
        page = new PDPage(new PDRectangle());
        document.addPage(page);
        font = PDType0Font.load(document, new File(getRutaFont()));
        contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
        mediaBox = page.getMediaBox();
    }
    public static int getPage_num() {
        return page_num;
    }

    public static void setPage_num(int page_num) {
        PDF.page_num = page_num;
    }

    public static PDType0Font getFont() {
        return font;
    }

    public static void setFont(PDType0Font font) {
        PDF.font = font;
    }

    public static int getFontSize() {
        return fontSize;
    }

    public static void setFontSize(int fontSize) throws IOException {
        PDF.fontSize = fontSize;
        contentStream.setFont(font, fontSize);
    }
    public static void newPage() throws IOException{
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        setFontSize(fontSize);
    }
    public static void newPageOffSeT() throws IOException{
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        //page = new PDPage(new PDRectangle(ANCHO, ALTO));
        page = new PDPage(new PDRectangle());
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        setFontSize(fontSize);
    }
    
    public static void setColor(Color color) throws IOException{
        contentStream.setNonStrokingColor(color); 
    }
    public static void setBold() throws IOException{
        font = PDType0Font.load(document, new File(getRutaFontBold()));
        contentStream.setFont(font, fontSize);
    }
    public static void setNormal() throws IOException{
        font = PDType0Font.load(document, new File(getRutaFont()));
        contentStream.setFont(font, fontSize);
    }
    public static int PositionMid(int x,int y){
        int resultado;
        resultado=y-x;
        resultado=y-(resultado/2);
        return resultado;
    }
    public void armarTabla(JSONArray array) throws JSONException{
        JSONArray head = array.getJSONArray(0);
        JSONArray body = array.getJSONArray(0);
        int width=0;
        
        for(int i=0;i<head.length();i++){
            width+=head.getJSONObject(i).getInt("width");
        }
        int restar=configWidth(width);
        for(int i=0;i<head.length();i++){
            width+=head.getJSONObject(i).getInt("width");
        }
    }
    
    public int configWidth(int width){
        int restar =0;
        int aux=0;
        if(width>ALTO_MARGEN){
            aux=width-ALTO_MARGEN;
            restar=aux/espacios.length;
        }
        return restar;
    }
    
    public static ArrayList<String> limitText(String text,int tamano){
        ArrayList list = new ArrayList<>();
        String mesenge = "";
        String[] list_aux = text.split(" ");
        for (int i = 0; i < list_aux.length; i++) {
            mesenge += list_aux[i]+" ";
            if (tamano <= mesenge.length()) {
                list.add(mesenge);
                mesenge = "";
            } else {
                if (i == list_aux.length - 1) {
                    list.add(mesenge);
                }
            }
        }
        return list;
    }
    public static ArrayList<String> truncarText(String text,int tamano){
        ArrayList list = new ArrayList<>();
        String mesenge = "";
        for (int i = 0; i < text.length(); i++) {
            mesenge += text.charAt(i);
            if((mesenge.length()>=tamano)){
                list.add(mesenge);
                mesenge="";
            }
            if(i>=(text.length()-1)){
                list.add(mesenge);
            }
        }
        return list;
    }
    public static String getMesNum(int num) {
        String mes ="";
        switch (num) {
            case 1:
                mes = "ENERO";
                break;
            case 2:
                mes = "FEBRERO";
                break;
            case 3:
                mes = "MARZO";
                break;
            case 4:
                mes = "ABRIL";
                break;
            case 5:
                mes = "MAYO";
                break;
            case 6:
                mes = "JUNIO";
                break;
            case 7:
                mes = "JULIO";
                break;
            case 8:
                mes = "AGOSTO";
                break;
            case 9:
                mes = "SEPTIEMBRE";
                break;
            case 10:
                mes = "OCTUBRE";
                break;
            case 11:
                mes = "NOVIEMBRE";
                break;
            case 12:
                mes = "DICIEMBRE";
                break;    
        }
        return mes;
    }

    public static String  getFechaLiteral(String fecha_date){
        String fecha ="";
        //2022-03-29T19:08:24
        String aux[]=fecha_date.split("T");
        aux = aux[0].split("-");
        fecha = aux[2]+" DE "+getMesNum(Integer.parseInt(aux[1]))+" DE "+aux[0];
        return fecha;
    }
    public static String  getFechaFormat(String fecha_date){
        String fecha ="";
        //2022-03-29T19:08:24
        String aux[]=fecha_date.split("T");
        String hora[]=aux[1].split(":");
        aux = aux[0].split("-");
        String estado="AM";
        if(Integer.parseInt(hora[0])>12){
            estado="PM";
        }
        fecha = aux[2]+"/"+aux[1]+"/"+aux[0] +" "+hora[0]+":"+hora[1]+" "+estado;
        return fecha;
    }
    public static String getNumFormat(double monto){
        String monto_total="";
        monto_total=myFormatter.format(monto);
        myFormatter.setMaximumFractionDigits(2);
        myFormatter.setMinimumFractionDigits(2);
       if (monto_total.equals("0")) {
            monto_total = monto_total + ".00";
        }
        String[] tamano = monto_total.split("\\.");
        if (tamano.length == 1) {
            monto_total = monto_total + ".00";
        } else {
            if (tamano[1].length() == 1) {
                monto_total = monto_total + "0";
            }
        }
        return monto_total;
    }
    
    public static String getNumFormat3(double monto){
        String monto_total="";
        monto_total=myFormatter3.format(monto);
        myFormatter3.setMaximumFractionDigits(3);
        myFormatter3.setMinimumFractionDigits(3);
       if (monto_total.equals("0")) {
            monto_total = monto_total + ".000";
        }
        String[] tamano = monto_total.split("\\.");
        if (tamano.length == 1) {
            monto_total = monto_total + ".000";
        } else {
            if (tamano[1].length() == 1) {
                monto_total = monto_total + "0";
            }
        }
        return monto_total;
    }
    
    private static byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }

    public static String getRutaFont() {
        return rutaFont;
    }

    public static void setRutaFont(String rutaFont) {
        PDF.rutaFont = rutaFont;
    }

    public static String getRutaFontBold() {
        return rutaFontBold;
    }

    public static void setRutaFontBold(String rutaFontBold) {
        PDF.rutaFontBold = rutaFontBold;
    }

    public static String getUrlImageAnulado() {
        return urlImageAnulado;
    }

    public static void setUrlImageAnulado(String urlImageAnulado) {
        PDF.urlImageAnulado = urlImageAnulado;
    }
    public static void drawLinePoint(float posy) throws IOException{
         int posLine=15;
        for(int m=0 ;m<23; m++){
            contentStream.moveTo(posLine, posy);
            contentStream.lineTo(posLine+6,posy);
            contentStream.stroke(); 
            posLine+=9;
        }
    }
    public static void drawPoints(float posy,int ancho) throws IOException{
        int posLine=15;
        String puntos="";
        for(int m=0 ;m<ancho; m++){
            puntos+=".";
        }
        write(15, posy, puntos);
    }
    public static int getHeight_RolloDPF(JSONObject contenido) throws JSONException {
        espacios = new int[8];
        espacios[0] = 20;
        espacios[1] = 90;
        espacios[2] = 150;
        espacios[3] = 230;
        espacios[4] = 410;
        espacios[5] = 470;
        espacios[6] = 530;
        espacios[7] = 592;
        numero_literal= new NumeroLiteral();
        
        JSONObject cabeza = contenido.getJSONObject("CABEZA");
        JSONArray cuerpo = contenido.getJSONArray("CUERPO");
        JSONObject casa = contenido.getJSONObject("CASA");


        float y = 0;
        y += 30;
        int codigo = cabeza.getInt("CODIGOSUCURSAL");

        String nombre = cabeza.getString("NOMBRERAZONSOCIAL");
        String cuf = cabeza.getString("CUF");
        String leyenda = cabeza.getString("LEYENDA");
        String estado = cabeza.getString("ESTADO");
        String casa_descripcion = "N/A";
        String casa_telefono = "N/A";

        double monto_total = 0.0;
        double descuento = 0.0;
        double monto_subTotal = 0.0;
        double monto_IBCF = 0.0;
        double gifCard = 0.0;

        if (!cabeza.isNull("MONTOTOTAL")) {
            monto_total = cabeza.getDouble("MONTOTOTAL");
        }
        if (!cabeza.isNull("DESCUENTOADICIONAL")) {
            descuento = cabeza.getDouble("DESCUENTOADICIONAL");
        }

        if (!cabeza.isNull("MONTOTOTALSUJETOIVA")) {
            monto_IBCF = cabeza.getDouble("MONTOTOTALSUJETOIVA");
        }
        if (!cabeza.isNull("MONTOGIFCARD")) {
            gifCard = cabeza.getDouble("MONTOGIFCARD");
        }

        if (!casa.isNull("DIRECCION")) {
            casa_descripcion = casa.getString("DIRECCION");
        }
        if (!casa.isNull("TELEFONO")) {
            casa_telefono = casa.getString("TELEFONO");
        }

        int complemento = 0;

        if (!cabeza.isNull("COMPLEMENTO")) {
            complemento = cabeza.getInt("COMPLEMENTO");
        }

        ArrayList<String> direccion = limitText(cabeza.getString("DIRECCION"), 30);
        ArrayList<String> list_casa_direccion = limitText(casa_descripcion, 30);

        String titulo_factura = "FACTURA";
        String sub_titulo_factura = "CON DERECHO A CRÉDITO FISCAL";

        //titulo_factura
        y += 9;
        //sub_titulo_factura
        y += 9;
 
        if (codigo == 0) {
            //razon_social
            y += 10;
            //casa_matriz

            y += 8;
            for (int j = 0; j < direccion.size(); j++) {
                y += 8;
            }
        } else {
            //razon_social
            y += 10;
            //sucursal
            y += 8;
            //telefono
            y += 8;
            for (int j = 0; j < list_casa_direccion.size(); j++) {
                //direccion
                y += 8;
            }
            y += 7;
            //razon_social
            y += 10;
            //casa
            y += 8;
            for (int j = 0; j < direccion.size(); j++) {
                y += 8;
            }
            y += 8;
        }
        y += 8;
        y += 8;
        y += 16;

        y = y + 9;
        y = y + 18;
        y = y + 9;
        ArrayList<String> array_cuf = truncarText(cuf, 30);
        float pos_cuf = y;
        for (int j = 0; j < array_cuf.size(); j++) {
            pos_cuf += 9;
        }
        y = pos_cuf;
        y += 9;
        y += 17;

        ArrayList<String> list_NOMBRE = limitText(nombre, 15);

        float aux_pos_nombre = y;

        for (int j = 0; j < list_NOMBRE.size(); j++) {
            aux_pos_nombre += 8;
        }
        y = aux_pos_nombre + 2;
        y += 10;
        y += 10;
        y += 9;
        y += 17;
        y += 10;

        for (int i = 0; i < cuerpo.length(); i++) {
            
            double monto_descuento = 0.0;
            if (!cuerpo.getJSONObject(i).isNull("MONTODESCUENTO")) {
                monto_descuento = cuerpo.getJSONObject(i).getDouble("MONTODESCUENTO");
            }

            int codigo_producto = cuerpo.getJSONObject(i).getInt("CODIGOPRODUCTO");
            int cantidad = cuerpo.getJSONObject(i).getInt("CANTIDAD");
            double precio_unitario = cuerpo.getJSONObject(i).getDouble("PRECIOUNITARIO");
            double subtotal = cuerpo.getJSONObject(i).getDouble("SUBTOTAL");
            String descrip = cuerpo.getJSONObject(i).getString("DESCRIPCION");
            String descrip_medida = cuerpo.getJSONObject(i).getString("DESCRIP_MEDIDA");

            monto_subTotal += subtotal;

            ArrayList<String> list_descrip = limitText(codigo_producto + "-" + descrip, 35);

            float aux_pos = y;

            for (int j = 0; j < list_descrip.size(); j++) {
                aux_pos += 8;
            }
            y = aux_pos;
           
            y += 7;

        }
        y += 8;
        y += 10;

        y += 9;

        y += 9;
        y += 9;

        
        y += 9;

        
        y += 9;
        
        y += 9;
      

        y += 18;
        String literal_num = numero_literal.Convertir(myFormatter.format(monto_total), true);
        ArrayList<String> lista_literal = limitText("SON :" + literal_num, 60);
        float pos_num = y;
        for (int i = 0; i < lista_literal.size(); i++) {
            pos_num += 8;
        }
        y = pos_num;
        y += 17;

        String mensaje = "ESTA FACTURA CONTRIBUYE AL DESARROLLO DEL PAÍS, EL USO ILÍCITO SERÁ SANCIONADO PENALMENTE DE ACUERDO A LEY";
        ArrayList<String> lis_aux = limitText(mensaje, 40);
        float pos_aux = y;
        for (int i = 0; i < lis_aux.size(); i++) {
            pos_aux += 8;
        }
        y = pos_aux;
        y += 10;
        ArrayList<String> list_leyenda = limitText(leyenda, 40);
        for (int i = 0; i < list_leyenda.size(); i++) {
            y += 10;
        }

        String documento = "'Este Documento es la Representación Gráfica de un Documento Fiscal Digital emitido en una modalidad de facturacion en linea.'";
        String enlace = "www.impuestos.gob.bo";
        y += 2;
        if (estado.equals("enviado")) {
            documento = "Este documento es la Representación Gráfica de un Documento Fiscal Digital emitido fuera de linea, ";
            lis_aux = limitText(documento, 40);
            pos_aux = y;
            for (int i = 0; i < lis_aux.size(); i++) {
                pos_aux += 8;
            }
            y = pos_aux;
            y += 8;
            y += 8;

        } else {
            lis_aux = limitText(documento, 40);
            pos_aux = y;
            for (int i = 0; i < lis_aux.size(); i++) {
                pos_aux += 8;
            }
            y = pos_aux;
        }
        return Math.round(y+150);
    }
}
