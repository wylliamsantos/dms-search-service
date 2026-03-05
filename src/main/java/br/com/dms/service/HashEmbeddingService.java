package br.com.dms.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HashEmbeddingService {

    private static final int DIMENSIONS = 128;

    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        if (StringUtils.isBlank(text)) {
            return vector;
        }

        String[] tokens = text.toLowerCase(Locale.ROOT).split("\\W+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int index = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[index] += 1.0d;
        }

        normalize(vector);
        return vector;
    }

    public double cosineSimilarity(double[] left, double[] right) {
        int dimension = Math.min(left.length, right.length);
        if (dimension == 0) {
            return 0.0d;
        }

        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;

        for (int i = 0; i < dimension; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private void normalize(double[] vector) {
        double norm = 0.0d;
        for (double value : vector) {
            norm += value * value;
        }

        if (norm == 0.0d) {
            return;
        }

        double divisor = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / divisor;
        }
    }
}
