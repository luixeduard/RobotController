module com.uaemex.mx.robotcontroller {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires java.desktop;
    requires javafx.swing;


    opens com.uaemex.mx.robotcontroller to javafx.fxml;
    exports com.uaemex.mx.robotcontroller;
}