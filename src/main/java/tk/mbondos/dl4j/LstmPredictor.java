package tk.mbondos.dl4j;

import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tk.mbondos.CoinDeskData;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class LstmPredictor {
    private static final Logger log = LoggerFactory.getLogger(LstmPredictor.class);
    private int exampleLength = 6;
    private String file = new ClassPathResource("ohlc.csv").getFile().getAbsolutePath();
    private int batchSize = 64;

    private double splitRatio = 1;
    private int epochs = 100;
    private PriceCategory category = PriceCategory.CLOSE;
    private File locationToSave = new File("StockPriceLSTM_".concat(String.valueOf(category)).concat(".zip"));


    private ExchangeRateDataIterator iterator = new ExchangeRateDataIterator(file, batchSize, exampleLength, splitRatio, category);

    private CoinDeskData coinDeskData = new CoinDeskData();

    private MultiLayerNetwork network = LstmNetwork.buildLstmNetwork(iterator.inputColumns(), iterator.totalOutcomes());

    public LstmPredictor() throws IOException {
    }

    public void trainAndTest() throws IOException {
/*
        log.info("Training...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) {
                network.fit(iterator.next());
            }
            iterator.reset();
            network.rnnClearPreviousState();
        }
        log.info("Saving model...");

        ModelSerializer.writeModel(network, locationToSave, true);*/

/*
        log.info("Load model...");
        network = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

        log.info("Testing...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();
        double max = iterator.getMaxNum(category);
        double min = iterator.getMinNum(category);
        predictPriceOneAhead(network, test, max, min, category);
*/

        log.info("Done...");



    }

    public double predictOne() throws IOException {
        String fileName = new ClassPathResource(coinDeskData.getOhlcPriceDateRange(LocalDate.now().minusDays(exampleLength), LocalDate.now())).getFile().getAbsolutePath();

        MultiLayerNetwork network = ModelSerializer.restoreMultiLayerNetwork("src/main/resources/StockPriceLSTM_CLOSE.zip");

        double max = iterator.getMaxNum(category);
        double min = iterator.getMinNum(category);
        log.info("max : {}", max);
        log.info("min : {}", min);

        INDArray array = iterator.getInputData(fileName);

        System.out.println(array.length());
        double prediction = network.rnnTimeStep(array).getDouble(exampleLength - 1);
        log.info("prediction {}", prediction);
        double predictionNormalized = min + ( prediction - 0.1 ) * (max - min)  / 0.8;

        System.out.println("input data");
        System.out.println(iterator.getInputData(fileName));
        log.info("prediction normalized: {}", predictionNormalized);

        return predictionNormalized;
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
