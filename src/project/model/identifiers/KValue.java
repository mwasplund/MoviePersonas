package project.model.identifiers;

public class KValue extends Identifier<Integer> {

    public KValue(final Integer value) {
        super(value);
    }

    public static UserId KValue(final Integer value) {
        return new UserId(value);
    }
}