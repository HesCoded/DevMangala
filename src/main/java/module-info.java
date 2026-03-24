module me.hescoded.devmangala {
    requires javafx.controls;
    requires javafx.fxml;


    opens me.hescoded.devmangala to javafx.fxml;
    exports me.hescoded.devmangala;
}