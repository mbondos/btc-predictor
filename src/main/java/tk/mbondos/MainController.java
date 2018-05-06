package tk.mbondos;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tk.mbondos.dl4j.LstmPredictor;
import tk.mbondos.neuroph.NeuralNetworkBtcPredictor;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    public LineChart<?, ?> btcChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private MenuItem menuPredict;

    @FXML
    private MenuItem menuTest;

    private CoinDeskData coinDeskData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        coinDeskData = new CoinDeskData();

        btcChart.setMinHeight(550);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(6000);
        yAxis.setUpperBound(10000);
        yAxis.setTickUnit(100);

        //xAxis.setTickLabelGap(1);

        setUpTest(null);
    }

    public void setUpTest(ActionEvent event) {
        btcChart.getData().clear();
        btcChart.getData().addAll(
                new XYChart.Series("Neuroph MLP ", preparePredictionNeuroph(31, LocalDate.now().minusDays(31))),
                new XYChart.Series("DeepLearning4j ", preparePredictionDl4j(31, LocalDate.now().minusDays(31))),
                new XYChart.Series("CoinDesk API ", prepareData(coinDeskData.getClosePriceLast31Days()))
        );
    }

    public void setUpPrediction(ActionEvent event) {
        btcChart.getData().clear();
        btcChart.getData().addAll(
                new XYChart.Series("Neuroph MLP ", preparePredictionNeuroph(7, LocalDate.now())),
                new XYChart.Series("DeepLearning4j", preparePredictionDl4j(7, LocalDate.now()))
        );
    }


    private SortedList<XYChart.Data<String, Number>> prepareData(String dataSetPath) {
        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> sortedData = new SortedList<>(dataset, (data1, data2) -> {
            LocalDate date1 = LocalDate.parse(data1.getXValue());
            LocalDate date2 = LocalDate.parse(data2.getXValue());
            return date1.compareTo(date2);
        });

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(dataSetPath)));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String date = LocalDate.parse(tokens[0].substring(1, 11)).toString();
                if (tokens.length != 1) {
                    final XYChart.Data<String, Number> data = new XYChart.Data<>(date, Double.valueOf(tokens[1]));
                    data.setNode(
                            new HoveredThresholdNode(Double.valueOf(tokens[1]), 2));
                    dataset.add(data);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sortedData = sortedData.sorted();

        return sortedData;
    }

    private SortedList<XYChart.Data<String, Number>> preparePredictionNeuroph(int seriesLength, LocalDate startingDate) {
        startingDate = startingDate.minusDays(1);
        SortedList<XYChart.Data<String, Number>> sortedData = prepareData(coinDeskData.getClosePriceDateRange(startingDate.minusDays(5), startingDate));
        NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor();
        double[] inputData = new double[6];


        for (int i = 0; i < sortedData.size(); i++) {
            inputData[i] = sortedData.get(i).getYValue().doubleValue();
        }
        double[] predictSeries = predictor.predictSeries(inputData, seriesLength);

        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(dataset);

        for (int i = 0; i < predictSeries.length; i++) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(startingDate.plusDays(i + 1).toString(), predictSeries[i]);
            data.setNode(
                    new HoveredThresholdNode(predictSeries[i], 0));
            dataset.add(data);
        }


        return outputData;

    }

    private SortedList<XYChart.Data<String, Number>> preparePredictionDl4j(int seriesLength, LocalDate startingDate) {
        startingDate = startingDate.minusDays(1);
        LstmPredictor predictor = null;
        double[] predictSeries = new double[seriesLength];
        try {
            predictor = new LstmPredictor();
            predictSeries = predictor.predictSeries(seriesLength, startingDate);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(dataset);


        for (int i = 0; i < predictSeries.length; i++) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(startingDate.plusDays(i + 1).toString(), predictSeries[i]);
            data.setNode(
                    new HoveredThresholdNode(predictSeries[i], 1));
            dataset.add(data);
        }

        return outputData;
    }

    public void trainDl4j(ActionEvent event) {

        final Stage dialog = new Stage();
        TextArea textArea = new TextArea();
        PrintStream ps = new PrintStream(new Console(textArea));
        ProgressIndicator progressIndicator = new ProgressIndicator();
        System.setOut(ps);
        System.setErr(ps);
        dialog.setTitle("Trenowanie sieci");
        dialog.initModality(Modality.APPLICATION_MODAL);
        StackPane dialogVbox = new StackPane();
        dialogVbox.getChildren().addAll(progressIndicator, textArea);
        StackPane.setAlignment(progressIndicator, Pos.CENTER);
        Scene dialogScene = new Scene(dialogVbox, 1000, 300);
        dialog.setScene(dialogScene);
        dialog.show();

        textArea.setVisible(false);

        try {
            progressIndicator.setVisible(true);
            LstmPredictor predictor = new LstmPredictor();


            new Thread(() -> {
                try {
                    predictor.trainNetwork();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                textArea.setVisible(true);
                progressIndicator.setVisible(false);
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trainNeuroph(ActionEvent event) {
        final Stage dialog = new Stage();
        TextArea textArea = new TextArea();
        PrintStream ps = new PrintStream(new Console(textArea));
        ProgressIndicator progressIndicator = new ProgressIndicator();
        System.setOut(ps);
        System.setErr(ps);
        dialog.setTitle("Trenowanie sieci");
        dialog.initModality(Modality.APPLICATION_MODAL);
        VBox dialogVbox = new VBox(20);
        dialogVbox.getChildren().addAll(progressIndicator, textArea);
        Scene dialogScene = new Scene(dialogVbox, 1000, 300);
        dialog.setScene(dialogScene);
        dialog.show();

        textArea.setVisible(false);
        progressIndicator.setVisible(true);

        NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor();

  /*      new Thread(() -> {


        }).start();*/
        try {
            predictor.trainNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }

        textArea.setVisible(true);
        progressIndicator.setVisible(false);
        textArea.setVisible(true);
        progressIndicator.setVisible(false);

    }

    public void showAuthorDialog(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Autor");
        alert.setHeaderText("Autor: Maksymilian Bondos");
        //alert.setContentText("");
        alert.showAndWait();
    }

    public class Console extends OutputStream {
        private TextArea console;

        public Console(TextArea console) {
            this.console = console;
        }

        public void appendText(String valueOf) {
            Platform.runLater(() -> console.appendText(valueOf));
        }

        public void write(int b) throws IOException {
            appendText(String.valueOf((char) b));
        }
    }
}
