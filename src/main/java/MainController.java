import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

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

    CoinDeskData coinDeskData = new CoinDeskData();

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        String filename = coinDeskData.getBpiLast31Days();





       /* btcChart.getXAxis().setTickLabelsVisible(false);
        btcChart.getXAxis().setOpacity(0);*/

        //btcChart.setCreateSymbols(false);


        btcChart.getData().addAll(new XYChart.Series(prepareData(filename, "CoinDesk")), new XYChart.Series(preparePrediction()));

    }

    private SortedList<XYChart.Data<String, Number>> prepareData(String dataSetPath, String seriesName) {
        ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> sortedData = new SortedList<>(data, (data1, data2) -> {
            LocalDate date1 = LocalDate.parse(data1.getXValue());
            LocalDate date2 = LocalDate.parse(data2.getXValue());
            return date1.compareTo(date2);
        });
        XYChart.Series series = new XYChart.Series();
        series.setName(seriesName);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(dataSetPath));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(":");
                String date = tokens[0].substring(1, 11);
                if (tokens.length != 1) {
                    data.add(new XYChart.Data(date, Double.valueOf(tokens[1])));
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

    private SortedList<XYChart.Data<String, Number>> preparePrediction() {
        SortedList<XYChart.Data<String, Number>> sortedData = prepareData(coinDeskData.getBpiUsingDateRange(LocalDate.now().minusDays(6), LocalDate.now()), "SSN");
        NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor();
        double[] inputData = new double[6];


        for (int i = 0; i < sortedData.size(); i++) {
            inputData[i] = sortedData.get(i).getYValue().doubleValue();
        }

        ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();
        SortedList<XYChart.Data<String, Number>> outputData = new SortedList<>(data);

        data.add(new XYChart.Data(LocalDate.now().toString(), predictor.predict(inputData)));

        return outputData;

    }
}
