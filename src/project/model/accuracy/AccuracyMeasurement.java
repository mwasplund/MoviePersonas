package project.model.accuracy;

import java.util.ArrayList;
import java.util.List;

public class AccuracyMeasurement {

    private final List<Double> rawErrors;
    private final String measurementName;

    private Double meanAbsoluteError;
    private Double meanSquaredError;
    private boolean accuracyCalculated = false;

    public AccuracyMeasurement(final String measurementName) {
        this.rawErrors = new ArrayList<Double>();
        this.measurementName = measurementName;
    }

    public String getName() {
        return this.measurementName;
    }

    public synchronized void addRawError(final Double error) {
        this.rawErrors.add(error);
    }

    public void calculateAccuracy() {
        int totalMeasurementCount = 0;
        Double absErrorSum = 0.0;
        Double squaredErrorSum = 0.0;
        for (final Double error : this.rawErrors) {
            absErrorSum += Math.abs( error );
            squaredErrorSum += Math.pow( error, 2.0 );
            totalMeasurementCount++;
        }
        this.meanAbsoluteError = (1.0 / totalMeasurementCount) * absErrorSum;
        this.meanSquaredError = Math.sqrt( squaredErrorSum / totalMeasurementCount );
        this.accuracyCalculated = true;
    }

    public int getSampleCount() {
        return this.rawErrors.size();
    }

    public Double getMeanAbsoluteError() {
        if (this.accuracyCalculated) {
            return this.meanAbsoluteError;
        } else {
            throw new IllegalArgumentException("Error not yet calculated.");
        }
    }

    public Double getRootMeanSquaredError() {
        if (this.accuracyCalculated) {
            return this.meanSquaredError;
        } else {
            throw new IllegalArgumentException("Error not yet calculated.");
        }
    }
}