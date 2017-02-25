package project.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import project.model.identifiers.Genre;
import project.model.identifiers.MovieId;
import project.model.movie.Movie;

public class GenreMap {

    private final Map<Genre, Set<Movie>> moviesByGenre;
    private final Map<MovieId, Movie> movies;

    public GenreMap() {
        this.moviesByGenre = new HashMap<Genre, Set<Movie>>();
        this.movies = new HashMap<MovieId, Movie>();
    }
}