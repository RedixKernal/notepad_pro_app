module notesapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires java.net.http;
    requires com.google.gson;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    opens com.ravi.notesapp.app to javafx.graphics;
    opens com.ravi.notesapp.controller to javafx.fxml;
    opens com.ravi.notesapp.model to com.google.gson;
    
    exports com.ravi.notesapp.app;
}
