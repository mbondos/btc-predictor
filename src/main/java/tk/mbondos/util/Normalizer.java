package tk.mbondos.util;

public class Normalizer {
    public static double normalizeValue(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    public static double deNormalizeValue(double input, double min, double max) {
        return min + (input - 0.1) * (max - min) / 0.8;
    }

}
