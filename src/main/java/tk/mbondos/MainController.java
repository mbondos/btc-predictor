package tk.mbondos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import tk.mbondos.dl4j.LstmPredictor;
import tk.mbondos.neuroph.NeuralNetworkBtcPredictor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    public LineChart<?, ?> btcChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    private CoinDeskData coinDeskData = new CoinDeskData();

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        String filename = coinDeskData.getClosePriceLast31Days();

       /* btcChart.getXAxis().setTickLabelsVisible(false);
        btcChart.getXAxis().setOpacity(0);*/

        //btcChart.setCreateSymbols(false);
        coinDeskData.getOhlcPriceLast31Days();


        btcChart.getData().addAll(new XYChart.Series("CoinDesk", prepareData(filename)), new XYChart.Series("Neuroph", preparePredictionNeuroph()));

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
            reader = new BufferedReader(new FileReader(dataSetPath));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String date = tokens[0].substring(1, 11);
                if (tokens.length != 1) {
                    final XYChart.Data<String , Number> data = new XYChart.Data<>(date, Double.valueOf(tokens[1]));
                    data.setNode(
                            new HoveredThresholdNode(Double.valueOf(tokens[1])));
                    dataset.add(data);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return sortedData;
    }

    private SortedList<XYChart.Data<String, Number>> preparePredictionNeuroph() {
        SortedList<XYChart.Data<String, Number>> sortedData = prepareData(coinDeskData.getClosePriceDateRange(LocalDate.now().minusDays(6), LocalDate.now()));
        NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor();
        double[] inputData = new double[6];


        for (int i = 0; i < sortedData.size(); i++) {
            inputData[i] = sortedData.get(i).getYValue().doubleValue();
        }

        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(dataset);


        double predictedValue = predictor.predict(inputData);

        XYChart.Data<String, Number> data = new XYChart.Data<>(LocalDate.now().toString(), predictedValue);
        data.setNode(
                new HoveredThresholdNode(predictedValue));

        dataset.add(data);


        return outputData;

    }

    private SortedList<XYChart.Data<String, Number>> preparePredictionDl4j() {
        SortedList<XYChart.Data<String, Number>> sortedData = prepareData(coinDeskData.getClosePriceDateRange(LocalDate.now().minusDays(6), LocalDate.now()));
        LstmPredictor predictor = null;
        try {
            predictor = new LstmPredictor();
        } catch (IOException e) {
            e.printStackTrace();
        }
        double[] inputData = new double[6];


        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(dataset);


        double predictedValue =

        XYChart.Data<String, Number> data = new XYChart.Data<>(LocalDate.now().toString(), predictedValue);
        data.setNode(
                new HoveredThresholdNode(predictedValue));

        dataset.add(data);


        return outputData;

    }
}
