package project.model;

import org.apache.commons.math3.ml.clustering.Clusterable;

import project.model.identifiers.UserId;

public class ReducedUserProfile implements Clusterable {

    private final UserId userId;
    private final double[] point;

    public ReducedUserProfile(final UserId userId,
                              final double[] point) {
        this.userId = userId;
        this.point = point;
    }

    @Override
    public double[] getPoint() {
        return this.point;
    }

    public UserId getUserId() {
        return this.userId;
    }

}