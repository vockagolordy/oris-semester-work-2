module ru.itis.scrabble {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jakarta.persistence;


    opens ru.itis.scrabble to javafx.fxml;
    exports ru.itis.scrabble;
}