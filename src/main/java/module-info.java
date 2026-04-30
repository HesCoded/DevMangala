module me.hescoded.devmangala {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;

    opens me.hescoded.devmangala to javafx.fxml;
    exports me.hescoded.devmangala;
}