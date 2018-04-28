import com.google.common.collect.ImmutableMap;
import com.opencsv.CSVReader;
import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ExchangeRateDataIterator implements DataSetIterator {
    private final int VECTOR_SIZE = 4;
    private final Map<PriceCategory, Integer> featureMapIndex = ImmutableMap.of(
            PriceCategory.OPEN, 0, PriceCategory.HIGH, 1,
            PriceCategory.LOW, 2, PriceCategory.CLOSE, 3);

    private int miniBatchSize;
    private int exampleLength = 22; // default 22 days for prediction
    private int predictLength = 1; //default 1 day ahead predict

    private double[] minArray = new double[VECTOR_SIZE];
    private double[] maxArray = new double[VECTOR_SIZE];

    private PriceCategory category;

    private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();

    private List<ExchangeRateData> train;

    private List<Pair<INDArray, INDArray>> test;

    public ExchangeRateDataIterator(String filename, int miniBatchSize, int exampleLength, double splitRatio, PriceCategory category) {
        List<ExchangeRateData> exchangeRateData = readDataFromFile(filename);
        this.miniBatchSize = miniBatchSize;
        this.exampleLength = exampleLength;
        this.category = category;
        int split = (int) Math.round(exchangeRateData.size() * splitRatio);
        train = exchangeRateData.subList(0, split);
        test = generateTestDataSet(exchangeRateData.subList(split, exchangeRateData.size()));
        initializeOffsets();
    }

    private List<Pair<INDArray, INDArray>> generateTestDataSet(List<ExchangeRateData> exchangeRateDataList) {
        int window = exampleLength + predictLength;
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();
        for (int i = 0; i < exchangeRateDataList.size() - window; i++) {
            INDArray input = Nd4j.create(new int[] {exampleLength, VECTOR_SIZE}, 'f');
            for (int j = i; j < i + exampleLength; j++) {
                ExchangeRateData data = exchangeRateDataList.get(j);
                input.putScalar(new int[] {j - i, 0}, (data.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]));
                input.putScalar(new int[] {j - i, 1}, (data.getHigh() - minArray[1]) / (maxArray[1] - minArray[1]));
                input.putScalar(new int[] {j - i, 2}, (data.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
                input.putScalar(new int[] {j - i, 3}, (data.getClose() - minArray[3]) / (maxArray[3] - minArray[3]));
            }
            ExchangeRateData data = exchangeRateDataList.get(i + exampleLength);
            INDArray label;

            if (category.equals(PriceCategory.ALL)) {
                label = Nd4j.create(new int[]{VECTOR_SIZE}, 'f'); // ordering is set as 'f', faster construct
                label.putScalar(new int[] {0}, data.getOpen());
                label.putScalar(new int[] {1}, data.getHigh());
                label.putScalar(new int[] {2}, data.getLow());
                label.putScalar(new int[] {3}, data.getClose());

            } else {
                label = Nd4j.create(new int[] {1}, 'f');
                switch (category) {
                    case OPEN: label.putScalar(new int[] {0}, data.getOpen()); break;
                    case HIGH: label.putScalar(new int[] {0}, data.getHigh()); break;
                    case LOW: label.putScalar(new int[] {0}, data.getLow()); break;
                    case CLOSE: label.putScalar(new int[] {0}, data.getClose()); break;
                    default: throw new NoSuchElementException();
                }
            }
            test.add(new Pair<>(input, label));
        }
        return test;
    }

    private List<ExchangeRateData> readDataFromFile(String filename) {
        List<ExchangeRateData> exchangeRateDataList = new ArrayList<>();

        try {
            for (int i = 0; i < maxArray.length; i++) {
                maxArray[i] = Double.MIN_VALUE;
                minArray[i] = Double.MAX_VALUE;
            }
            List<String[]> list = new CSVReader(new FileReader(filename)).readAll();
            for (String[] arr: list) {
                double[] nums = new double[VECTOR_SIZE];
                for (int i = 0; i < arr.length - 1; i++) {
                    nums[i] = Double.valueOf(arr[i + 1]);
                    if (nums[i] > maxArray[i]) maxArray[i] = nums[i];
                    if (nums[i] < minArray[i]) minArray[i] = nums[i];
                }
                exchangeRateDataList.add(new ExchangeRateData(arr[0], nums[0], nums[1], nums[2], nums[3]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exchangeRateDataList;
    }

    private void initializeOffsets() {
        exampleStartOffsets.clear();
        int window = exampleLength + predictLength;
        for (int i = 0; i < train.size() - window; i++) {
            exampleStartOffsets.add(i);
        }
    }

    public double[] getMinArray() {
        return minArray;
    }

    public double[] getMaxArray() {
        return maxArray;
    }

    public List<Pair<INDArray, INDArray>> getTestDataSet() {
        return test;
    }

    public double getMaxNum (PriceCategory category) { return maxArray[featureMapIndex.get(category)]; }

    public double getMinNum (PriceCategory category) { return minArray[featureMapIndex.get(category)]; }

    @Override
    public DataSet next(int num) {
        if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();
        int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());
        INDArray input = Nd4j.create(new int[] {actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
        INDArray label;

        if (category.equals(PriceCategory.ALL)) label = Nd4j.create(new int[] {actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
        else label = Nd4j.create(new int[] {actualMiniBatchSize, predictLength, exampleLength}, 'f');

        for (int index = 0; index < actualMiniBatchSize; index++) {
            int startIdx = exampleStartOffsets.removeFirst();
            int endIdx = startIdx + exampleLength;
            ExchangeRateData curData = train.get(startIdx);
            ExchangeRateData nextData;
            for (int i = startIdx; i < endIdx; i++) {
                int c = i - startIdx;
                input.putScalar(new int[] {index, 0, c}, (curData.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]));
                input.putScalar(new int[] {index, 1, c}, (curData.getHigh() - minArray[1]) / (maxArray[1] - minArray[1]));
                input.putScalar(new int[] {index, 2, c}, (curData.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
                input.putScalar(new int[] {index, 3, c}, (curData.getClose() - minArray[3]) / (maxArray[3] - minArray[3]));
                nextData = train.get(i + 1);
                if (category.equals(PriceCategory.ALL)) {
                    label.putScalar(new int[] {index, 0, c}, (nextData.getOpen() - minArray[1]) / (maxArray[1] - minArray[1]));
                    label.putScalar(new int[] {index, 1, c}, (nextData.getHigh() - minArray[2]) / (maxArray[2] - minArray[2]));
                    label.putScalar(new int[] {index, 2, c}, (nextData.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
                    label.putScalar(new int[] {index, 3, c}, (nextData.getClose() - minArray[3]) / (maxArray[3] - minArray[3]));
                } else {
                    label.putScalar(new int[]{index, 0, c}, feedLabel(nextData));
                }
                curData = nextData;
            }
            if (exampleStartOffsets.size() == 0) break;
        }
        return new DataSet(input, label);
    }

    private double feedLabel(ExchangeRateData data) {
        double value;
        switch (category) {
            case OPEN: value = (data.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]); break;
            case HIGH: value = (data.getHigh() - minArray[1]) / (maxArray[1] - minArray[1]); break;
            case LOW: value = (data.getLow() - minArray[2]) / (maxArray[2] - minArray[2]); break;
            case CLOSE: value = (data.getClose() - minArray[3]) / (maxArray[3] - minArray[3]); break;
            default: throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public int totalExamples() {
        return train.size() - exampleLength - predictLength;
    }

    @Override
    public int inputColumns() {
        return VECTOR_SIZE;
    }

    @Override
    public int totalOutcomes() {
        if (this.category.equals(PriceCategory.ALL)) return VECTOR_SIZE;
        else return predictLength;
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {
        initializeOffsets();
    }

    @Override
    public int batch() {
        return miniBatchSize;
    }

    @Override
    public int cursor() {
        return totalExamples() - exampleStartOffsets.size();
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public boolean hasNext() {
        return exampleStartOffsets.size() > 0;
    }

    @Override
    public DataSet next() {
        return next(miniBatchSize);
    }
}
