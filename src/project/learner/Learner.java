package project.learner;

public interface Learner<T> {

    public T learn();

    public Boolean isComplete();

    public String getStatus();
}