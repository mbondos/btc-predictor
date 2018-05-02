package tk.mbondos.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class NormalizerTest {

    @Test
    public void normalizeValue() {
        double min = 0.0505;
        double max = 19343.04;
        double input = 9543.99;
        double normalized = Normalizer.normalizeValue(input, min, max);
        double deNormalized = min + (normalized - 0.1) * (max - min) / 0.8;
        assertEquals(deNormalized, input, 0.99);

        input = 13.99;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = min + (normalized - 0.1) * (max - min) / 0.8;
        assertEquals(deNormalized, input, 0.99);

        input = 2222.22;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = min + (normalized - 0.1) * (max - min) / 0.8;
        assertEquals(deNormalized, input, 0.99);

        input = 1111.111111;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = min + (normalized - 0.1) * (max - min) / 0.8;
        assertEquals(deNormalized, input, 0.99);

        input = 9999.99;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = min + (normalized - 0.1) * (max - min) / 0.8;
        assertEquals(deNormalized, input, 0.99);

    }

    @Test
    public void deNormalizeValue() {
        double min = 0.0505;
        double max = 19343.04;
        double input = 9543.99;
        double normalized = Normalizer.normalizeValue(input, min, max);
        double deNormalized = Normalizer.deNormalizeValue(normalized, min, max);
        assertEquals(deNormalized, input, 0.99);

        input = 13.99;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = Normalizer.deNormalizeValue(normalized, min, max);
        assertEquals(deNormalized, input, 0.99);

        input = 2222.22;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = Normalizer.deNormalizeValue(normalized, min, max);
        assertEquals(deNormalized, input, 0.99);

        input = 1111.111111;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = Normalizer.deNormalizeValue(normalized, min, max);
        assertEquals(deNormalized, input, 0.99);

        input = 9999.99;
        normalized = Normalizer.normalizeValue(input, min, max);
        deNormalized = Normalizer.deNormalizeValue(normalized, min, max);
        assertEquals(deNormalized, input, 0.99);

    }
}