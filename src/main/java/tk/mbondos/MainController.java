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
import org.nd4j.linalg.io.ClassPathResource;
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



       /* btcChart.getXAxis().setTickLabelsVisible(false);
        btcChart.getXAxis().setOpacity(0);*/


        btcChart.getData().addAll(
                new XYChart.Series("CoinDesk", prepareData(coinDeskData.getClosePriceLast31Days())),
                new XYChart.Series("Neuroph", preparePredictionNeuroph()),
                new XYChart.Series("Dl4j", preparePredictionDl4j())
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
            reader = new BufferedReader(new FileReader(new ClassPathResource(dataSetPath).getFile()));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String date = tokens[0].substring(1, 11);
                if (tokens.length != 1) {
                    final XYChart.Data<String , Number> data = new XYChart.Data<>(date, Double.valueOf(tokens[1]));
                    data.setNode(
                            new HoveredThresholdNode(Double.valueOf(tokens[1]), 0));
                    dataset.add(data);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return sortedData;
    }

    private SortedList<XYChart.Data<String, Number>> preparePredictionNeuroph() {
        int seriesLength = 31;
        SortedList<XYChart.Data<String, Number>> sortedData = prepareData(coinDeskData.getClosePriceDateRange(LocalDate.now().minusDays(seriesLength + 5), LocalDate.now().minusDays(seriesLength)));
        NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor();
        double[] inputData = new double[6];


        for (int i = 0; i < sortedData.size(); i++) {
            inputData[i] = sortedData.get(i).getYValue().doubleValue();
        }
        double[] predictSeries = predictor.predictSeries(inputData,seriesLength);

        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(dataset);
        LocalDate startingDate = LocalDate.now().minusDays(seriesLength);

        for (int i = 0; i < predictSeries.length; i++) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(startingDate.plusDays(i).toString(), predictSeries[i]);
            data.setNode(
                    new HoveredThresholdNode(predictSeries[i], 1));
            dataset.add(data);
        }




        return outputData;

    }

    private SortedList<XYChart.Data<String, Number>> preparePredictionDl4j() {
        LstmPredictor predictor = null;
        int seriesLength = 31;
        double[] predictSeries = new double[seriesLength];
        try {
            predictor = new LstmPredictor();
            //predictor.trainAndTest();
            predictSeries = predictor.predictSeries(seriesLength);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ObservableList<XYChart.Data<String, Number>> dataset = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(dataset);


        LocalDate startingDate = LocalDate.now().minusDays(seriesLength);

        for (int i = 0; i < predictSeries.length; i++) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(startingDate.plusDays(i).toString(), predictSeries[i]);
            data.setNode(
                    new HoveredThresholdNode(predictSeries[i], 2));
            dataset.add(data);
        };
        return outputData;
    }
}
