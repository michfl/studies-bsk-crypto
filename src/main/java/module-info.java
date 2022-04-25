module pl.edu.pg.eti.ksr.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;

    opens pl.edu.pg.eti.ksr.project to javafx.fxml;
    exports pl.edu.pg.eti.ksr.project;
}
