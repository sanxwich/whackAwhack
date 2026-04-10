module com.whackawhack.whackawhack {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.whackawhack.whackawhack to javafx.fxml;
    exports com.whackawhack.whackawhack;
}