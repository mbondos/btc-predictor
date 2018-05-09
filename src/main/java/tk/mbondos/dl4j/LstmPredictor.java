package tk.mbondos.dl4j;

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

public class LstmPredictor {
    private static final Logger log = LoggerFactory.getLogger(LstmPredictor.class);
    private int exampleLength = 24;
    private String file = "data/btc_ohlc_lifetime.csv";
    private int batchSize = 64;
    private double splitRatio = 1; // Ratio of train to test data. Use 1 for 100% train data.
    private PriceCategory category = PriceCategory.CLOSE;
    private File networkFileLocation = new File("data/StockPriceLSTM_CLOSE.zip");

    private ExchangeRateDataIterator iterator = new ExchangeRateDataIterator(file, batchSize, exampleLength, splitRatio, category);

    private CoinDeskData coinDeskData = new CoinDeskData();

    private MultiLayerNetwork network = LstmNetwork.buildLstmNetwork(iterator.inputColumns(), iterator.totalOutcomes());

    public LstmPredictor() {
        CoinDeskData coinDeskData = new CoinDeskData();
        file = coinDeskData.getOhlcPriceLifetime();
    }

    /**
     * Train network over number of epochs and reset iterator.
     *
     * @throws IOException
     */
    public void trainNetwork() throws IOException {
        log.info("Training...");
        int epochs = 100;
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

    /**
     * Predict tomorrows value and clear network state.
     *
     * @return Value of next day.
     * @throws IOException Throw if ANN file not found.
     */
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
        network.rnnClearPreviousState();
        log.info("prediction {}", prediction);
        double predictionNormalized = Normalizer.deNormalizeValue(prediction, min, max);

        log.info("prediction normalized: {}", predictionNormalized);

        return predictionNormalized;
    }

    /**
     * Predicts values of specified number of days starting from specified date.
     *
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

}
