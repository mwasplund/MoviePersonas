package project.model.identifiers;

public class MovieId extends Identifier<Integer> {

    public MovieId(final Integer value) {
        super(value);
    }

    public static MovieId valueOf(final Integer value) {
        return new MovieId(value);
    }
}