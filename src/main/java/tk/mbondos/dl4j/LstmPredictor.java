package tk.mbondos.dl4j;

import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tk.mbondos.CoinDeskData;
import tk.mbondos.util.Normalizer;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class LstmPredictor {
    private static final Logger log = LoggerFactory.getLogger(LstmPredictor.class);
    private int exampleLength = 24;
    private String file = "data/btc_ohlc_lifetime.csv";
    private int batchSize = 64;

    private double splitRatio = 1; // Ratio of train to test data. Use 1 for 100% train data.
    private int epochs = 100;
    private PriceCategory category = PriceCategory.CLOSE;
    private File networkFileLocation = new File("data/StockPriceLSTM_CLOSE.zip");


    private ExchangeRateDataIterator iterator = new ExchangeRateDataIterator(file, batchSize, exampleLength, splitRatio, category);

    private CoinDeskData coinDeskData = new CoinDeskData();

    private MultiLayerNetwork network = LstmNetwork.buildLstmNetwork(iterator.inputColumns(), iterator.totalOutcomes());

    public LstmPredictor() {
        CoinDeskData coinDeskData = new CoinDeskData();
        file = coinDeskData.getOhlcPriceLifetime();
    }

    public void trainNetwork() throws IOException {
        log.info("Training...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) {
                network.fit(iterator.next());
            }
            iterator.reset();
            network.rnnClearPreviousState();
        }
        log.info("Saving model...");

        ModelSerializer.writeModel(network, networkFileLocation, true);

        log.info("Training successful.");
    }

    public void testNetwork() throws IOException {
        log.info("Load model...");
        network = ModelSerializer.restoreMultiLayerNetwork(networkFileLocation);

        log.info("Testing...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();
        double max = iterator.getMaxNum(category);
        double min = iterator.getMinNum(category);
        predictPriceOneAhead(network, test, max, min, category);

        log.info("Done...");
    }

    public double predictOne() throws IOException {
        String fileName = new ClassPathResource(coinDeskData.getOhlcPriceDateRange(LocalDate.now().minusDays(exampleLength), LocalDate.now())).getFile().getAbsolutePath();

        MultiLayerNetwork network =
                ModelSerializer.restoreMultiLayerNetwork(networkFileLocation);

        double max = iterator.getMaxNum(category);
        double min = iterator.getMinNum(category);
        log.info("max : {}", max);
        log.info("min : {}", min);

        INDArray array = iterator.getInputData(fileName);

        double prediction = network.rnnTimeStep(array).getDouble(exampleLength - 1);
        log.info("prediction {}", prediction);
        double predictionNormalized = Normalizer.deNormalizeValue(prediction, min, max);

        log.info("prediction normalized: {}", predictionNormalized);

        return predictionNormalized;
    }

    /**
     * Predicts values of specified number of days starting from specified date.
     * @param seriesLength Number of time steps to predict.
     * @param startingDate Starting day of prediction.
     * @return Returns array of predicted values.
     * @throws IOException Throw if ANN file not found.
     */
    public double[] predictSeries(int seriesLength, LocalDate startingDate) throws IOException {
        double[] output = new double[seriesLength];

        String fileName =
                coinDeskData.getOhlcPriceDateRange(
                        startingDate.minusDays(exampleLength - 1),
                        startingDate);

        MultiLayerNetwork network =
                ModelSerializer.restoreMultiLayerNetwork(networkFileLocation);

        double max = iterator.getMaxNum(category);
        double min = iterator.getMinNum(category);
        log.info("max : {}", max);
        log.info("min : {}", min);

        INDArray array = iterator.getInputData(fileName);

        for (int i = 0; i < seriesLength; i++) {
            output[i] = Normalizer.deNormalizeValue(
                    network.rnnTimeStep(array).getDouble(exampleLength - 1),
                    min,
                    max
            );
            log.info("Predicted: {}", output[i]++);
        }

        return output;
    }



    private void predictPriceOneAhead(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min, PriceCategory category) {
        double[] predicts = new double[testData.size()];
        double[] actuals = new double[testData.size()];
        for (int i = 0; i < testData.size(); i++) {
            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getDouble(exampleLength - 1) * (max - min) + min;
            actuals[i] = testData.get(i).getValue().getDouble(0);
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict,Actual");
        for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "," + actuals[i]);

    }


}
