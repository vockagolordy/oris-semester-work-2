module ru.itis.scrabble {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jakarta.persistence;
    requires jbcrypt;


    opens ru.itis.scrabble.controllers to javafx.fxml;
    opens ru.itis.scrabble to javafx.fxml;

    exports ru.itis.scrabble;
    exports ru.itis.scrabble.controllers;
}