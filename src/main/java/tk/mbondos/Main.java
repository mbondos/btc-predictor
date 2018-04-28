package tk.mbondos;

import javafx.application.Application;
import javafx.stage.Stage;
import tk.mbondos.dl4j.LstmPredictor;

public class Main extends Application {

    public void start(Stage primaryStage) throws Exception {
/*
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        primaryStage.setTitle("Bitcoin Predictor");
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.show();
*/

/*        tk.mbondos.NeuralNetworkBtcPredictor nn = new tk.mbondos.NeuralNetworkBtcPredictor();
        nn.prepareData();*/

        LstmPredictor predictor = new LstmPredictor();

        predictor.trainAndTest();






    }




}
