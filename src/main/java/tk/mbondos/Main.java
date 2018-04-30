package tk.mbondos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tk.mbondos.dl4j.LstmPredictor;

public class Main extends Application {

    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("main.fxml"));
        primaryStage.setTitle("Bitcoin Predictor");
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add("style.css");
        primaryStage.setScene(scene);
        primaryStage.show();

/*        LstmPredictor predictor = new LstmPredictor();
        predictor.trainAndTest();*/

    }




}
