import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;


public class DeepLearningPredictor {
    private static final double learningRate = 0.05;
    private static final int iterations = 1;
    private static final int seed = 12345;

    private static final int lstmLayer1Size = 256;
    private static final int lstmLayer2Size = 256;
    private static final int denseLayerSize = 32;
    private static final double dropoutRatio = 0.2;
    private static final int truncatedBPTTLength = 22;


        public static MultiLayerNetwork buildLstmNetworks(int nIn, int nOut) {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(seed)
                    .iterations(iterations)
                    .learningRate(learningRate)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .weightInit(WeightInit.XAVIER)
                    .updater(Updater.RMSPROP)
                    .regularization(true)
                    .l2(1e-4)
                    .list()
                    .layer(0, new GravesLSTM.Builder()
                            .nIn(nIn)
                            .nOut(lstmLayer1Size)
                            .activation(Activation.TANH)
                            .gateActivationFunction(Activation.HARDSIGMOID)
                            .dropOut(dropoutRatio)
                            .build())
                    .layer(1, new GravesLSTM.Builder()
                            .nIn(lstmLayer1Size)
                            .nOut(lstmLayer2Size)
                            .activation(Activation.TANH)
                            .gateActivationFunction(Activation.HARDSIGMOID)
                            .dropOut(dropoutRatio)
                            .build())
                    .layer(2, new DenseLayer.Builder()
                            .nIn(lstmLayer2Size)
                            .nOut(denseLayerSize)
                            .activation(Activation.RELU)
                            .build())
                    .layer(3, new RnnOutputLayer.Builder()
                            .nIn(denseLayerSize)
                            .nOut(nOut)
                            .activation(Activation.IDENTITY)
                            .lossFunction(LossFunctions.LossFunction.MSE)
                            .build())
                    .backpropType(BackpropType.TruncatedBPTT)
                    .tBPTTForwardLength(truncatedBPTTLength)
                    .tBPTTBackwardLength(truncatedBPTTLength)
                    .pretrain(false)
                    .backprop(true)
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();
            net.setListeners(new ScoreIterationListener(100));
            return net;
        }


/*

        network.init();
        network.setListeners(new ScoreIterationListener(1));



        File saveLocation = new File("dl4j_network.zip");
        try {
            ModelSerializer.writeModel(network, saveLocation, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int nEpochs = 4;
        String str = "Test set evaluation at epoch %d: Accuracy = %.2f, F1 = %.2f";

        DataSet trainData = trainingIterator.next();
        DataSet testData = testIterator.next();



        for (int i = 0; i < nEpochs; i++) {
            network.fit(trainData);

            RegressionEvaluation evaluation = new RegressionEvaluation(1);
            INDArray features = testData.getFeatureMatrix();

            INDArray lablse = testData.getLabels();
            INDArray predicted = network.output(features, false);

            evaluation.evalTimeSeries(lablse, predicted);
            log.info("\n" + evaluation.stats());





        */
/*    RegressionEvaluation evaluation = network.evaluateRegression(testIterator);
            log.info("\n" + evaluation.stats());
            testIterator.reset();
            trainingIterator.reset();*//*

        }
    }

    private DataSetIterator getTrainingData(String filename, int minBatchSize, int labelIndex) {
        SequenceRecordReader recordReader = new CSVSequenceRecordReader(0, ",");
        DataSetIterator iterator = null;


        try {
            recordReader.initialize(new FileSplit(new File(filename)));

            iterator = new SequenceRecordReaderDataSetIterator(recordReader, minBatchSize, 1, labelIndex, true);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return iterator;
    }

    private Dataset<org.apache.spark.sql.Row> getDataSpark(String filename) {
        SparkSession spark = SparkSession.builder().master("local").appName("DataProcess")
                .getOrCreate();
        Dataset<org.apache.spark.sql.Row> data = null;


            data = spark.read().format("csv").option("header", true).option("timestampFormat", "yyyy-MM-dd HH:mm:ss")
                    .load(new File(filename).getAbsolutePath())
                    //.withColumn("date", functions.col("date").cast("LocalDate")).drop("date")
                    .withColumn("openPrice", functions.col("open").cast("double")).drop("open")
                    .withColumn("highPrice", functions.col("high").cast("double")).drop("high")
                    .withColumn("lowPrice", functions.col("low").cast("double")).drop("low")
                    .withColumn("closePrice", functions.col("close").cast("double")).drop("close")
                    .toDF("date", "open", "high", "low", "close");
            data.show();




        return data;
    }

*/



}
