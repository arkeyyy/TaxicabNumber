module org.example.taxicab_number {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.taxicab_number to javafx.fxml;
    exports org.example.taxicab_number;
}