package project.model.identifiers;

public class MovieName extends Identifier<String> {

    public MovieName(final String value) {
        super(value);
    }

    public static MovieName valueOf(final String value) {
        return new MovieName(value);
    }
}