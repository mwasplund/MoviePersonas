package project.main.runnable;

import project.model.CollaborativeFilter;

public class UpdateTestingProgressRunnable implements Runnable {

    final CollaborativeFilter filter;
    final long startTime;

    public UpdateTestingProgressRunnable(final CollaborativeFilter filter) {
        this.filter = filter;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        long lastRunTime = System.currentTimeMillis() - 60000;
        while (!this.filter.isComplete()) {
            try {
                if (lastRunTime + 60000 < System.currentTimeMillis()) {
                    this.filter.getAccuracy().calculateAccuracy();
                    System.out.println(String.format("Elapsed time: %.2fs, completed %d of %d (%.2f%%) tests (cache size: {%d}, current MAE: {%.2f}, current RMSE {%.2f})",
                                ((System.currentTimeMillis() - this.startTime) / 1000.0),
                                this.filter.getTestRowStatus(),
                                this.filter.getTestRowCount(),
                                (100.0 * this.filter.getTestRowStatus() / this.filter.getTestRowCount()),
                                0,
                                this.filter.getAccuracy().getMeanAbsoluteError(),
                                this.filter.getAccuracy().getRootMeanSquaredError()));
                    lastRunTime = System.currentTimeMillis();
                }
                Thread.sleep(1000);
            } catch (final Exception e) { ; }
        }
        System.out.println(String.format("Completed testing after: %.2fs, (cache size: {%d})",
                ((System.currentTimeMillis() - this.startTime) / 1000.0),
                0));
    }
}