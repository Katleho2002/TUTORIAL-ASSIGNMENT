module com.example.vehicle {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql; // Add this line
    opens com.example.vehicle to javafx.fxml;
    exports com.example.vehicle;
}
