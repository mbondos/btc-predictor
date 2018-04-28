import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;

import java.io.*;
import java.util.LinkedList;

public class NeuralNetworkBtcPredictor {
    private int slidingWindowSize = 6;
    private double max = 0;
    private double min = Double.MAX_VALUE;
    private String rawDataFilePath = "input/trainingData.csv";

    private String learningDataFilePath = "input/learningDataNotNormalised.csv";
    private String neuralNetworkModelFilePath = "stockPredictor.nnet";

/*    public static void main(String[] args) throws IOException {
        NeuralNetworkBtcPredictor predictor = new NeuralNetworkBtcPredictor(6, "input/trainingData.csv");
        predictor.prepareData();

       *//* System.out.println("Training starting");
        predictor.trainNetwork();*//*

        System.out.println("Testing network");
        predictor.testNetwork();

    }*/

    public NeuralNetworkBtcPredictor() {
    }

    public NeuralNetworkBtcPredictor(int slidingWindowSize, String rawDataFilePath) {
        this.slidingWindowSize = slidingWindowSize;
        this.rawDataFilePath = rawDataFilePath;
    }

    void prepareData() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(rawDataFilePath));
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
        BufferedWriter writer = new BufferedWriter(new FileWriter(learningDataFilePath));
        BufferedWriter minMaxWriter = new BufferedWriter(new FileWriter("min_max.txt"));

        LinkedList<Double> valuesQueue = new LinkedList<Double>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 2) {
                    double crtValue = Double.valueOf(tokens[1]);
                    double normalizedValue = normalizeValue(crtValue);
                    valuesQueue.add(normalizedValue);

                    if (valuesQueue.size() == slidingWindowSize + 1) {
                        String valueLine = valuesQueue.toString().replaceAll(
                                "\\[|\\]", "");
                        writer.write(valueLine);
                        writer.newLine();
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

        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    private double deNormalizeValue(double input) {
        validateMinMax();
        return min + (input - 0.1) * (max - min) / 0.8;
    }

    private void validateMinMax() {
        if (max == 0 || min == Double.MAX_VALUE) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader("min_max.txt"));

                min = Double.valueOf(bufferedReader.readLine());
                max = Double.valueOf(bufferedReader.readLine());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    void trainNetwork() throws IOException {
        NeuralNetwork<BackPropagation> neuralNetwork =
                new MultiLayerPerceptron(slidingWindowSize, 2 * slidingWindowSize + 1, 1);

        int maxIterations = 10000;
        double learningRate = 0.5;
        double maxError = 0.00001;
        SupervisedLearning learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);
        learningRule.addListener(new LearningEventListener() {
            public void handleLearningEvent(LearningEvent learningEvent) {
                SupervisedLearning rule = (SupervisedLearning) learningEvent.getSource();
                System.out.println("Network error for interation "
                        + rule.getCurrentIteration() + ": "
                        + rule.getTotalNetworkError());
            }
        });

        DataSet trainingSet = loadTrainingData(learningDataFilePath);
        neuralNetwork.learn(trainingSet);
        neuralNetwork.save(neuralNetworkModelFilePath);
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
                    trainValues[i] = Double.valueOf(tokens[i]);
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

    public double predict(double[] inputData) {
        NeuralNetwork neuralNetwork = NeuralNetwork.createFromFile(neuralNetworkModelFilePath);
        if (inputData.length != 6) {
            throw new IllegalArgumentException("Must be exactly 6 entries.");
        }

        double[] normalizedInput = new double[6];

        for (int i = 0; i < inputData.length; i++) {
            normalizedInput[i] = normalizeValue(inputData[i]);
        }

        neuralNetwork.setInput(normalizedInput);

        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();
        
        return deNormalizeValue(networkOutput[0]);
    }

    void testNetwork() {
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
        System.out.println("Expected value  : 6855.4");
        System.out.println("Predicted value : "
                + deNormalizeValue(networkOutput[0]));
    }

}
