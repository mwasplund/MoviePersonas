package project.learner.collaborative;

import java.util.List;

import project.model.CollaborativeFilter;
import project.model.netflix.UserRating;

public class NetflixCollaborativeLearner extends CollaborativeFilterLearner {

    protected final int totalRowCount;
    protected int rowsProcessed;
    protected CollaborativeFilter currentFilter;
    protected String state = "INIT";

    protected final List<UserRating> records;

    public NetflixCollaborativeLearner(final List<UserRating> records) {
        this.records = records;
        this.totalRowCount = records.size();
        this.rowsProcessed = 0;
    }

    @Override
    public CollaborativeFilter learn() {
        this.state = "LOADING";
        this.currentFilter = new CollaborativeFilter();
        for (final UserRating record : this.records) {
            this.currentFilter.addRating( record );
            this.rowsProcessed++;
        }
        System.out.println("Calculating user average ratings.");

        this.state = "AVERAGES";
        this.currentFilter.calculateUserAverages();
        System.out.println(String.format(
                "User average ratings calculated successfully (%d of %d).",
                this.currentFilter.getAveragesRowStatus(),
                this.currentFilter.getAveragesRowCount()));

        this.completionStatus = true;
        return this.currentFilter;
    }

    @Override
    public String getStatus() {
        String retString = "";
        switch (this.state) {
        case "INIT":
            retString = "Learning not yet started";
            break;
        case "LOADING":
            retString = String.format("Loading input data, %d of %d (%.2f%%) complete",
                                this.rowsProcessed,
                                this.totalRowCount,
                                (100.0 * this.rowsProcessed / this.totalRowCount));
            break;
        case "AVERAGES":
            retString = String.format("Calculating user average values, %d of %d (%.2f%%) complete",
                                this.currentFilter.getAveragesRowStatus(),
                                this.currentFilter.getAveragesRowCount(),
                                (100.0 * this.currentFilter.getAveragesRowStatus() / this.currentFilter.getAveragesRowCount()));
            break;
        default:
            retString = "Complete";
        }

        return retString;
    }
}