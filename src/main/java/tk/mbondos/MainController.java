package tk.mbondos;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tk.mbondos.dl4j.LstmPredictor;
import tk.mbondos.neuroph.NeuralNetworkBtcPredictor;

import java.io.*;
import java.time.LocalDate;

public class MainController {
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

    public void initialize(){


        //Make sure directory "data" is present in relative path
        File file1 = new File("data/");
        file1.mkdirs();
        coinDeskData = new CoinDeskData();

        btcChart.setMinHeight(550);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(6000);
        yAxis.setUpperBound(10000);
        yAxis.setTickUnit(100);

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
                String date = LocalDate.parse(tokens[0]).toString();
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
        double[] predictSeries = new double[0];
        try {
            predictSeries = predictor.predictSeries(inputData, seriesLength);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Nie znaleziono pliku z siecią Neuroph. \nWybierz odpowiednią opcję w menu \"Trenuj\".");
        }

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
        double[] predictSeries = new double[0];
        try {
            predictor = new LstmPredictor();
            predictSeries = predictor.predictSeries(seriesLength, startingDate);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Nie znaleziono pliku z siecią Deeplearnign4j. \nWybierz odpowiednią opcję w menu \"Trenuj\".");
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
        System.setOut(ps);
        System.setErr(ps);
        dialog.setTitle("Trenowanie sieci");
        dialog.initModality(Modality.APPLICATION_MODAL);
        StackPane dialogPane = new StackPane();
        dialogPane.getChildren().addAll(textArea);
        Scene dialogScene = new Scene(dialogPane, 1000, 300);
        dialog.setScene(dialogScene);
        dialog.show();

        new Thread(() -> {
                try {
                    LstmPredictor predictor = new LstmPredictor();
                    predictor.trainNetwork();
                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorDialog("Błąd podczas trenowania sieci: " + e.getMessage());
                    dialog.close();
                }
            }).start();
        dialog.setOnCloseRequest(event1 -> setUpTest(null));
    }

    public void trainNeuroph(ActionEvent event) {
        final Stage dialog = new Stage();
        TextArea textArea = new TextArea();
        PrintStream ps = new PrintStream(new Console(textArea));
        System.setOut(ps);
        System.setErr(ps);
        dialog.setTitle("Trenowanie sieci");
        dialog.initModality(Modality.APPLICATION_MODAL);
        StackPane dialogPane = new StackPane();
        dialogPane.getChildren().addAll(textArea);
        Scene dialogScene = new Scene(dialogPane, 1000, 300);
        dialog.setScene(dialogScene);
        dialog.show();

        new Thread(() -> {
            try {
                NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor();
                predictor.trainNetwork();
            } catch (IOException e) {
                showErrorDialog("Błąd podczas trenowania sieci: " + e.getMessage());
                e.printStackTrace();
                dialog.close();
            }
        }).start();

    }

    public void showAuthorDialog(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Autor");
        alert.setHeaderText("Autor: Maksymilian Bondos");
        //alert.setContentText("");
        alert.showAndWait();
    }
    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(message);
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
