package com.uaemex.mx.robotcontroller;

import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import com.fazecast.jSerialComm.*;
import javafx.scene.input.KeyEvent;
import javafx.embed.swing.SwingFXUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ControladorRobot implements Initializable {
    @FXML
    private Button start;
    @FXML
    private ComboBox<SerialPort> seriales;
    @FXML
    private ImageView imagen;
    @FXML
    private TextArea enviados = new TextArea();
    @FXML
    private TextArea recibidos;
    @FXML
    private TextField distancia;

    private SerialPort comPort;

    private ArrayList<Coordenada> obstaculosArray = new ArrayList<>();
    private ArrayList<Coordenada> obstaculosArrayN = new ArrayList<>();
    private ArrayList<Coordenada> posiciones = new ArrayList<>();
    private ArrayList<Coordenada> posicionesN = new ArrayList<>();

    private Coordenada F = new Coordenada(0, 1);
    private Coordenada I = new Coordenada(-1, 0);
    private Coordenada D = new Coordenada(1, 0);

    private int indexIzq = 0;
    private int indexFr = 1;
    private int indexDer = 2;

    private final Coordenada posActual = new Coordenada(0,0);
    private final Coordenada posActualRelativa = new Coordenada(0,0);

    private boolean inicial=true;
    private boolean anclaIzquierda = false;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        seriales.setItems(FXCollections.observableArrayList(SerialPort.getCommPorts()));
        distancia.addEventHandler(KeyEvent.KEY_TYPED, this::onlyNumbers);
        distancia.setText("15");
        vectorMult.add(new Coordenada(-1,0 ));  //Izquierda
        vectorMult.add(new Coordenada(0,1 ));   //Frente
        vectorMult.add(new Coordenada(1,0 ));   //Derecha
        vectorMult.add(new Coordenada(0,-1 ));  //Atras
    }


    private void onlyNumbers(KeyEvent evt){
        if(!(evt.getCode().isDigitKey() || evt.getCode().isKeypadKey())){
            evt.consume();
        }
    }

    public void inicializar(){
        obstaculosArray.clear();
        posiciones.clear();
        posActual.setX(0);
        posActual.setY(0);
        indexIzq = 0;
        indexFr = 1;
        indexDer = 2;
        enviados.clear();
        recibidos.clear();
    }

    @FXML
    public void empiezaTravesia(){
        if(start.getText().compareToIgnoreCase("¡Empezar!")==0) {
            if (!comPort.isOpen()) {
                comPort.openPort();
            }
            start.setText("PARA");
            start.setStyle("-fx-background-color: RED; ");
            seriales.setDisable(true);
            distancia.setDisable(true);
            solicitaDistancia();
        }else{
            start.setText("¡Empezar!");
            start.setStyle("-fx-background-color: GREEN; ");
            seriales.setDisable(false);
            distancia.setDisable(false);
            comPort.closePort();
            inicializar();
        }

    }

    @FXML
    public void changeSerial(){
        comPort = seriales.getSelectionModel().getSelectedItem();
        if(comPort!=null){
            if (comPort.isOpen()) {
                comPort.closePort();
            }
            comPort.openPort();
            comPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }
                @Override
                public void serialEvent(SerialPortEvent event)
                {
                    byte[] newData = event.getReceivedData();
                    StringBuilder data = new StringBuilder();
                    for (byte newDatum : newData)
                        if ((char) newDatum != '#')
                            data.append((char) newDatum);

                    if(data.length() != 0){
                        mueveRobot(data.toString());
                    }
                }
            });
        }
    }

    private void girarIndices(char c){
        switch (c) {
            case 'D' -> {
                indexDer = actualizaIndexDerecha(indexDer);
                indexFr = actualizaIndexDerecha(indexFr);
                indexIzq = actualizaIndexDerecha(indexIzq);
            }
            case 'I' -> {
                indexIzq = actualizaIndexIzquierda(indexIzq);
                indexFr = actualizaIndexIzquierda(indexFr);
                indexDer = actualizaIndexIzquierda(indexDer);
            }
        }
    }

    private void mueveRobot(String cad) {
        String[] cadenas = cad.split(",");
        recibidos.setText(recibidos.getText()+"\nIzq: "+cadenas[0]+"cm Fr: "+cadenas[1]+"cm Der: "+cadenas[2]+"cm");
        ArrayList<Double> distancias = new ArrayList<>();
        for(String cadena : cadenas){
            distancias.add(Double.parseDouble(cadena));
        }
        int tileSize = Integer.parseInt(distancia.getText());
        if(inicial && distancias.get(1)>=tileSize) {
            pintaMapa(distancias.get(0), distancias.get(1), distancias.get(2), tileSize);
            mueveRobotArduino("F", tileSize);
            posActual.setY(posActual.getY()+vectorMult.get(indexFr).getY());
            posActual.setX(posActual.getX()+vectorMult.get(indexFr).getY());
        }else if(inicial && distancias.get(1)<tileSize) {
            mueveRobotArduino("I", 90);
            girarIndices('D');
            inicial=false;
        }else if(distancias.get(0)>tileSize && distancias.get(1)>tileSize && distancias.get(2)>tileSize){
            if(anclaIzquierda) {
                mueveRobotArduino("D", 90);
                girarIndices('I');
                anclaIzquierda = false;
            }else{
                mueveRobotArduino("F", tileSize);
            }
        }else if(distancias.get(0)<tileSize && distancias.get(1)<tileSize){
            mueveRobotArduino("I", 90);
            girarIndices('D');
        }
        else if(distancias.get(1)>=tileSize){
            if(distancias.get(0)<tileSize){
                anclaIzquierda = true;
            }
            pintaMapa(distancias.get(0), distancias.get(1), distancias.get(2), tileSize);
            mueveRobotArduino("F", tileSize);
            posActual.setY(posActual.getY()+vectorMult.get(indexFr).getY());
            posActual.setX(posActual.getX()+vectorMult.get(indexFr).getY());
        }
        solicitaDistancia();
    }


    private void agregarObstaculoSinDuplicar(Coordenada coordenada){
        if(!obstaculosArray.contains(coordenada)){
            obstaculosArray.add(coordenada);
        }
    }

    private void agregarPosicionesSinDuplicar(Coordenada coordenada){
        if(!posiciones.contains(coordenada)){
            posiciones.add(coordenada);
        }
    }

    private BufferedImage creaImagen(){
        int minX = 0, maxX=0;
        int minY = 0, maxY=0;
        for(Coordenada c : obstaculosArray){
            if(c.getX()<minX){
                minX = c.getX();
            }else if(c.getX()>maxX){
                maxX = c.getX();
            }
            if(c.getY()<minY){
                minY = c.getY();
            }else if(c.getY()>maxY){
                maxY = c.getY();
            }
        }
        int x = maxX-minX;
        int y = maxY-minY;
        return new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
    }

    private void normalizaPosiciones(){
        int minX = 0;
        int minY = 0;
        for(Coordenada c : obstaculosArray){
            if(c.getX()<minX){
                minX = c.getX();
            }
            if(c.getY()<minY){
                minY = c.getY();
            }
        }
        obstaculosArrayN.clear();
        for(Coordenada c : obstaculosArray) {
            obstaculosArrayN.add(new Coordenada(c.getX()-minX, c.getY()-minY));
        }
        posicionesN.clear();
        for(Coordenada c : posiciones){
            posicionesN.add(new Coordenada(c.getX()-minX, c.getY()-minY));
        }
        posActualRelativa.setX(posActual.getX()-minX);

        posActualRelativa.setY(posActual.getY()-minY);
    }

    private void modificaImagenMapa(BufferedImage imagen){
        for(Coordenada coordenada : obstaculosArrayN){
            imagen.setRGB(coordenada.getX(), coordenada.getY(), Color.BLACK.getRGB());
        }
        for(Coordenada coordenada : posiciones){
            imagen.setRGB(coordenada.getX(), coordenada.getY(), Color.BLUE.getRGB());
        }
        imagen.setRGB(posActualRelativa.getX(), posActualRelativa.getY(), Color.RED.getRGB());
    }

    private void setImagen(BufferedImage mapa){
        imagen.setImage(SwingFXUtils.toFXImage(mapa, null));
    }

    private void pintaMapa(Double distIzq, Double distFr, Double distDer, int tileSize){
        int ObsIzq;
        int ObsFr;
        int ObsDer;
        if(distIzq==0){
            ObsIzq = 1;
        }else {
            ObsIzq =(int) (distIzq/tileSize) + 1;
        }
        if(distFr==0){
            ObsFr = 1;
        }else {
            ObsFr =(int) (distFr/tileSize)/ + 1;
        }
        if(distDer==0){
            ObsDer = 1;
        }else {
            ObsDer =(int) (distDer/tileSize) + 1;
        }
        Coordenada coord = new Coordenada(ObsIzq*vectorMult.get(indexIzq).getX()+posActual.getX(), ObsIzq*vectorMult.get(indexIzq).getY()+posActual.getY()); //EJ izq obstaculo en 5 Coordenada Izquierda = (-1, 0)  |   5*(-1, 0) = (-5, 0);
        agregarObstaculoSinDuplicar(coord);
        coord = new Coordenada(ObsFr*vectorMult.get(indexFr).getX()+posActual.getX(), ObsFr*vectorMult.get(indexFr).getY()+posActual.getY());
        agregarObstaculoSinDuplicar(coord);
        coord = new Coordenada(ObsDer*D.getX(), ObsDer*D.getY());
        agregarObstaculoSinDuplicar(coord);

        agregarPosicionesSinDuplicar(posActual);

        normalizaPosiciones();

        creaGrid();
    }


    private void mueveRobotArduino(String direccion, int medida){
        String comando = direccion + medida + "100#";
        comPort.writeBytes(comando.getBytes(), comando.length());
        enviados.setText(enviados.getText()+direccion+" "+medida+"\n");
    }

    private void solicitaDistancia(){
        String comando = "S#";
        comPort.writeBytes(comando.getBytes(), comando.length());
        enviados.setText(enviados.getText()+"Obtener distancia\n");
    }


}