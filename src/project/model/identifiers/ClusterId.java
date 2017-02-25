package project.model.identifiers;

public class ClusterId extends Identifier<Integer> {

    public ClusterId(final Integer value) {
        super(value);
    }

    public static ClusterId valueOf(final Integer value) {
        return new ClusterId(value);
    }
}