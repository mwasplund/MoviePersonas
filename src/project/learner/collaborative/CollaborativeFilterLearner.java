package project.learner.collaborative;

import project.learner.Learner;
import project.model.CollaborativeFilter;

public abstract class CollaborativeFilterLearner implements Learner<CollaborativeFilter> {

    protected Boolean completionStatus = false;

    @Override
    public abstract CollaborativeFilter learn();

    @Override
    public abstract String getStatus();

    @Override
    public Boolean isComplete() {
        return this.completionStatus;
    }
}