package view;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


/**
 * Created by Didoy on 10/11/2015.
 */
public class AdminFrame extends Stage {

    public AdminFrame(){
        BorderPane root = new BorderPane();

        Label label = new Label("THe server is now running");

        root.setCenter(label);
        Scene scene = new Scene(root,300,300);
        centerOnScreen();
        setScene(scene);

    }
}
