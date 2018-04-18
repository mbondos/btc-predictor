import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    public LineChart<?, ?> btcChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CoinDeskData coinDeskData = new CoinDeskData();

        String filename = coinDeskData.getBpiLast31Days();

        XYChart.Series chartData = prepareData(filename, "CoinDesk");


        btcChart.getXAxis().setTickLabelsVisible(false);
        btcChart.getXAxis().setOpacity(0);
        btcChart.setCreateSymbols(false);


        btcChart.getData().addAll(chartData);
    }

    public XYChart.Series prepareData(String dataSetPath, String seriesName) {
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
                    series.getData().add(new XYChart.Data(date, Double.valueOf(tokens[1])));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }





        return series;
    }
}
