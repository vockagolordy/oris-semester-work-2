module ru.itis.scrabble {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.itis.scrabble to javafx.fxml;
    exports ru.itis.scrabble;
}