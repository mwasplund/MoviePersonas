package project.model.identifiers;

public class UserId extends Identifier<Integer> {

    public UserId(final Integer value) {
        super(value);
    }

    public static UserId valueOf(final Integer value) {
        return new UserId(value);
    }
}