package project.model.netflix;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import project.model.identifiers.MovieId;
import project.model.identifiers.UserId;

public class UserRating {

    private final UserId userId;
    private final MovieId movieId;
    private final Double rating;

    public UserRating(final UserId userId,
                      final MovieId movieId,
                      final Double rating) {
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
    }

    public UserId getUserId() {
        return this.userId;
    }

    public MovieId getMovieId() {
        return this.movieId;
    }

    public Double getRating() {
        return this.rating;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.userId)
                .append(this.movieId)
                .build();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof UserRating))
            return false;
        if (other == this)
            return true;

        final UserRating rhs = (UserRating) other;
        return new EqualsBuilder()
                        .append(this.userId, rhs.getUserId())
                        .append(this.movieId, rhs.getMovieId())
                        .isEquals();
    }

    @Override
    public String toString() {
        return String.format("{user: %d, movie %d, rating: %.2f}",
                        this.userId.getValue(),
                        this.movieId.getValue(),
                        this.rating);
    }
}