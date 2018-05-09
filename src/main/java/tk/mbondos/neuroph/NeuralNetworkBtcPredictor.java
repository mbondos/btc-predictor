package tk.mbondos.neuroph;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tk.mbondos.CoinDeskData;
import tk.mbondos.util.Normalizer;

import java.io.*;
import java.util.LinkedList;

public class NeuralNetworkBtcPredictor {
    private static final Logger log = LoggerFactory.getLogger(NeuralNetworkBtcPredictor.class);
    private int slidingWindowSize = 6;
    private double max = 0;
    private double min = Double.MAX_VALUE;

    private String rawDataFilePath;
    private String minMaxFilename = "data/min_max.txt";
    private String learningDataFilePath = "data/learningData.csv";
    private String neuralNetworkModelFilePath = "data/stockPredictor.nnet";


    public NeuralNetworkBtcPredictor() {
        CoinDeskData coinDeskData = new CoinDeskData();
        rawDataFilePath = coinDeskData.getClosePriceLifetime();
    }

    public NeuralNetworkBtcPredictor(int slidingWindowSize, String rawDataFilePath) {
        this.slidingWindowSize = slidingWindowSize;
        this.rawDataFilePath = rawDataFilePath;
    }

    public void prepareData() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(rawDataFilePath)));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 2) {
                    double crtValue = Double.valueOf(tokens[1]);
                    if (crtValue > max) {
                        max = crtValue;
                    }
                    if (crtValue < min) {
                        min = crtValue;
                    }
                }
            }
        } finally {
            reader.close();
        }
        reader = new BufferedReader(new FileReader(rawDataFilePath));
        File file = new File(learningDataFilePath);
        file.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        File minMaxFile = new File(minMaxFilename);
        BufferedWriter minMaxWriter = new BufferedWriter(new FileWriter(minMaxFile));

        LinkedList<Double> valuesQueue = new LinkedList<Double>();
        try {
            String line;
            boolean notFirstLine = false;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 2) {
                    double crtValue = Double.valueOf(tokens[1]);
                    double normalizedValue = normalizeValue(crtValue);
                    valuesQueue.add(normalizedValue);

                    if (valuesQueue.size() == slidingWindowSize + 1) {
                        String valueLine = valuesQueue.toString().replaceAll(
                                "\\[|\\]", "");
                        if (notFirstLine) {
                            writer.newLine();
                        } else {
                            notFirstLine = true;
                        }
                        writer.write(valueLine);
                        valuesQueue.removeFirst();
                    }
                }
            }
            minMaxWriter.write("" + min);
            minMaxWriter.newLine();
            minMaxWriter.write("" + max);
            writer.flush();
            minMaxWriter.flush();
        } finally {
            reader.close();
            writer.close();
            minMaxWriter.close();
        }
    }

    private double normalizeValue(double input) {
        validateMinMax();

        return Normalizer.normalizeValue(input, min, max);
    }

    private double deNormalizeValue(double input) {
        validateMinMax();
        return Normalizer.deNormalizeValue(input, min, max);
    }

    private void validateMinMax() {
        if (max == 0 || min == Double.MAX_VALUE) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(minMaxFilename));

                min = Double.valueOf(bufferedReader.readLine());
                max = Double.valueOf(bufferedReader.readLine());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void trainNetwork() throws IOException {
        NeuralNetwork<BackPropagation> neuralNetwork =
                new MultiLayerPerceptron(slidingWindowSize, 2 * slidingWindowSize + 1, 1);

        int maxIterations = 1000;
        double learningRate = 0.5;
        double maxError = 0.00001;
        SupervisedLearning learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);
        learningRule.addListener(learningEvent -> {
            SupervisedLearning rule = (SupervisedLearning) learningEvent.getSource();
            log.info("Network error for interation {} : {}",
                     rule.getCurrentIteration(),
                     rule.getTotalNetworkError());
        });

        prepareData();
        DataSet trainingSet = loadTrainingData(learningDataFilePath);
        neuralNetwork.learn(trainingSet);
        neuralNetwork.save(neuralNetworkModelFilePath);
        log.info("Training successful.");
    }

    private DataSet loadTrainingData(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        DataSet trainingSet = new DataSet(slidingWindowSize, 1);

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");

                double trainValues[] = new double[slidingWindowSize];
                for (int i = 0; i < slidingWindowSize; i++) {
                    trainValues[i] = Double.valueOf(tokens[1]);
                }
                double expectedValue[] = new double[]{
                        Double.valueOf(tokens[slidingWindowSize])
                };
                trainingSet.addRow(new DataSetRow(trainValues, expectedValue));
            }
        } finally {
            reader.close();
        }
        return trainingSet;
    }

    public double predict(double[] inputData) throws IOException {
        File file = new File(neuralNetworkModelFilePath);
        if (!file.exists()) {
            throw new IOException("Neural network file not found.");
        }

        NeuralNetwork neuralNetwork = NeuralNetwork.createFromFile(file);
        if (neuralNetwork == null) {
            throw new RuntimeException("Błąd wczytywania sieci z pliku");
        }
        if (inputData.length != slidingWindowSize) {
            throw new IllegalArgumentException("Length of array must be exactly " + slidingWindowSize + ".(Same as slidingWindowsSize)");
        }
        double[] normalizedInput = new double[6];

        for (int i = 0; i < inputData.length; i++) {
            normalizedInput[i] = normalizeValue(inputData[i]);
        }

        neuralNetwork.setInput(normalizedInput);

        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();

        log.info("Predicted: {}", deNormalizeValue(networkOutput[0]));

        return deNormalizeValue(networkOutput[0]);
    }

    public double[] predictSeries(double[] inputData, int seriesLength) throws IOException {
        double[] output = new double[seriesLength];
        for (int i = 0; i < seriesLength; i++) {
            output[i] = predict(inputData);
            inputData = shiftLeft(inputData);
            inputData[slidingWindowSize - 1] = output[i];
        }

        return output;
    }

    public double[] shiftLeft(double[] nums) {
        if (nums == null || nums.length <= 1) {
            return nums;
        }
        double start = nums[0];
        System.arraycopy(nums, 1, nums, 0, nums.length - 1);
        nums[nums.length - 1] = start;
        return nums;
    }

    public void testNetwork() {
        NeuralNetwork neuralNetwork = NeuralNetwork.createFromFile(neuralNetworkModelFilePath);
        neuralNetwork.setInput(
                normalizeValue(6844.32),
                normalizeValue(6926.02),
                normalizeValue(6816.74),
                normalizeValue(7049.79),
                normalizeValue(7417.89),
                normalizeValue(6789.3));

        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();
        log.info("Expected value  : 6855.4 \n Predicted value {}: ",
                 deNormalizeValue(networkOutput[0]));
    }

}
