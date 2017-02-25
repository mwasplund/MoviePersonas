package project.model.movie;

import java.util.HashSet;
import java.util.Set;

import project.model.identifiers.Genre;
import project.model.identifiers.MovieId;
import project.model.identifiers.MovieName;

public class Movie {

    private final MovieId id;
    private final MovieName name;
    private final Set<Genre> genres;

    public Movie(final MovieId id,
                 final MovieName name) {
        this.id = id;
        this.name = name;
        this.genres = new HashSet<Genre>();
    }

    public Movie withGenre(final Genre genre) {
        this.genres.add(genre);
        return this;
    }

    public MovieId getId() {
        return this.id;
    }

    public MovieName getName() {
        return this.name;
    }

    public Set<Genre> getGenres() {
        return this.genres;
    }
}