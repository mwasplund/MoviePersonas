package project.model.identifiers;

public class Genre extends Identifier<String> {

    public Genre(final String value) {
        super(value);
    }

    public static Genre valueOf(final String value) {
        return new Genre(value);
    }
}