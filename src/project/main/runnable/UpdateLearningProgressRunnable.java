package project.main.runnable;

import project.learner.Learner;

public class UpdateLearningProgressRunnable implements Runnable {

    final Learner learner;
    final long startTime;

    public UpdateLearningProgressRunnable(final Learner learner) {
        this.learner = learner;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!this.learner.isComplete()) {
            System.out.println(String.format("Elapsed time: %.2fs, status: %s",
                    ((System.currentTimeMillis() - this.startTime) / 1000.0),
                    this.learner.getStatus()));
            try {
                Thread.sleep(10000);
            } catch (final InterruptedException e) { ; }
        }
    }
}