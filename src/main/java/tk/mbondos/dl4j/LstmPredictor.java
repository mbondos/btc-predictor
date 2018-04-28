package tk.mbondos.dl4j;

import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LstmPredictor {
    private static final Logger log = LoggerFactory.getLogger(LstmPredictor.class);
    int exampleLength = 22;


    public void trainAndTest() throws IOException {
        String file = new ClassPathResource("ohlc.csv").getFile().getAbsolutePath();
        int batchSize = 64;

        double splitRatio = 0.9;
        int epochs = 100;
        PriceCategory category = PriceCategory.CLOSE;
        File locationToSave = new File("src/main/resources/StockPriceLSTM_".concat(String.valueOf(category)).concat(".zip"));

        log.info("Create dataSet iterator...");
        ExchangeRateDataIterator iterator = new ExchangeRateDataIterator(file, batchSize, exampleLength, splitRatio, category);
        log.info("Load test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();
        log.info("Build lstm networks...");
        MultiLayerNetwork network = LstmNetwork.buildLstmNetwork(iterator.inputColumns(), iterator.totalOutcomes());
        log.info("Training...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) {
                network.fit(iterator.next());
            }
            iterator.reset();
            network.rnnClearPreviousState();
        }
        log.info("Saving model...");

        ModelSerializer.writeModel(network, locationToSave, true);

        log.info("Load model...");
        network = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

        log.info("Testing...");
        double max = iterator.getMaxNum(category);
        double min = iterator.getMinNum(category);
        predictPriceOneAhead(network, test, max, min, category);

        log.info("Done...");

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
