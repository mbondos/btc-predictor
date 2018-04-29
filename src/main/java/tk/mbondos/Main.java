package tk.mbondos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tk.mbondos.dl4j.LstmPredictor;
import tk.mbondos.neuroph.NeuralNetworkBtcPredictor;

import java.time.LocalDate;

public class Main extends Application {

    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("main.fxml"));
        primaryStage.setTitle("Bitcoin Predictor");
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.show();

       /* NeuralNetworkBtcPredictor nn = new tk.mbondos.neuroph.NeuralNetworkBtcPredictor();
        nn.prepareData();*/


        LstmPredictor predictor = new LstmPredictor();

        predictor.trainAndTest();

    }




}
