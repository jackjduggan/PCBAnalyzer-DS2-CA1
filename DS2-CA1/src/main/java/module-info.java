module com.dugganjack.pcbanalyzer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.dugganjack.pcbanalyzer to javafx.fxml;
    exports com.dugganjack.pcbanalyzer;
}