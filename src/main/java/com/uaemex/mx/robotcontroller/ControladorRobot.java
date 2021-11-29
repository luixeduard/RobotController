package com.uaemex.mx.robotcontroller;

import javafx.collections.FXCollections;
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
    private TextArea enviados;
    @FXML
    private TextArea recibidos;
    @FXML
    private TextField distancia;

    private SerialPort comPort;

    private boolean ejecutar=true;
    private String cad = "";

    private ArrayList<Coordenada> obstaculosArray = new ArrayList<>();
    private ArrayList<Coordenada> obstaculosArrayN = new ArrayList<>();
    private ArrayList<Coordenada> posiciones = new ArrayList<>();
    private ArrayList<Coordenada> posicionesN = new ArrayList<>();

    private Coordenada F = new Coordenada(0, 1);
    private Coordenada I = new Coordenada(-1, 0);
    private Coordenada D = new Coordenada(1, 0);

    private Coordenada posActual = new Coordenada(0,0);
    private Coordenada posActualRelativa = new Coordenada(0,0);
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        seriales.setItems(FXCollections.observableArrayList(SerialPort.getCommPorts()));
        distancia.addEventHandler(KeyEvent.KEY_TYPED, event -> this.onlyNumbers(event));
    }


    private void onlyNumbers(KeyEvent evt){
        if(!evt.getCode().isDigitKey()){
            evt.consume();
        }
    }


    @FXML
    public void empiezaTravesia(){
        if(start.getText().compareToIgnoreCase("¡Empieza!")==0) {
            start.setText("PARA");
            start.setStyle("-fx-background-color: RED; ");
            seriales.setDisable(true);
            distancia.setDisable(true);
            solicitaDistancia();
        }else{
            start.setText("¡Empieza!");
            start.setStyle("-fx-background-color: GREEN; ");
            seriales.setDisable(false);
            distancia.setDisable(false);
            comPort.closePort();
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
                public void serialEvent(SerialPortEvent event){
                    byte[] newData = event.getReceivedData();
                    cad = "";
                    for (byte newDatum : newData) {
                        cad += (char) newDatum;
                    }
                    if(ejecutar)
                        mueveRobot(cad);
                }
            });
        }
    }

    private void mueveRobot(String cad){
        String[] cadenas = cad.split(",");
        recibidos.setText(recibidos.getText()+"\nIzq: "+cadenas[0]+"cm Fr: "+cadenas[1]+"cm Der: "+cadenas[2]+"cm");
        ArrayList<Double> distancias = new ArrayList<>();
        for(String cadena : cadenas){
            distancias.add(Double.parseDouble(cadena));
        }
        int tileSize = Integer.parseInt(distancia.getText());
        if(distancias.get(1)>=tileSize){
            pintaMapa(distancias.get(0), distancias.get(1), distancias.get(2), tileSize);
            mueveRobotArduino('F', tileSize);
            posActual.setX(posActual.getX()+F.getX());
            posActual.setY(posActual.getY()+F.getY());
        }
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
        obstaculosArrayN.clear();
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
        int ObsIzq = (int)(tileSize/distIzq)+1;
        int ObsFr = (int)(tileSize/distFr)+1;
        int ObsDer = (int)(tileSize/distDer)+1;
        Coordenada coord = new Coordenada(ObsIzq*I.getX(), ObsIzq*I.getY());
        agregarObstaculoSinDuplicar(coord);
        coord = new Coordenada(ObsFr*F.getX(), ObsFr*F.getY());
        agregarObstaculoSinDuplicar(coord);
        coord = new Coordenada(ObsDer*D.getX(), ObsDer*D.getY());
        agregarObstaculoSinDuplicar(coord);
        agregarPosicionesSinDuplicar(posActual);
        BufferedImage imagen = creaImagen();
        normalizaPosiciones();
        modificaImagenMapa(imagen);
        setImagen(imagen);
    }


    private void mueveRobotArduino(char direccion, int medida){
        String comando = direccion + medida +"#";
        comPort.writeBytes(comando.getBytes(), comando.length());
        enviados.setText(enviados.getText()+comando);
    }

    private void solicitaDistancia(){
        String comando = "S#";
        comPort.writeBytes(comando.getBytes(), comando.length());
        enviados.setText(enviados.getText()+comando);
    }


}