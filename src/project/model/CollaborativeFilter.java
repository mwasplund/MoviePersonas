package project.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import project.model.accuracy.AccuracyMeasurement;
import project.model.identifiers.MovieId;
import project.model.identifiers.UserId;
import project.model.netflix.UserRating;

public class CollaborativeFilter {

    protected final Map<MovieId, Set<UserRating>> ratingsByMovie;
    protected final Map<UserId, Map<MovieId, UserRating>> ratingsByUser;
    protected final Map<UserId, Set<MovieId>> moviesByUser;
    protected final Map<UserId, Double> averageUserVotes;

    protected int averagesRowCount = 0;
    protected int averagesRowStatus = 0;
    protected boolean completionStatus = false;
    protected int maxUserId = 0;

    private AccuracyMeasurement accuracy = new AccuracyMeasurement("default");

    public CollaborativeFilter() {
        this.ratingsByMovie = new HashMap<MovieId, Set<UserRating>>();
        this.ratingsByUser = new HashMap<UserId, Map<MovieId, UserRating>>();
        this.moviesByUser = new HashMap<UserId, Set<MovieId>>();
        this.averageUserVotes = new HashMap<UserId, Double>();
    }

    public void addRating(final UserRating rating) {
        // deal with adding this to the dataset per-movie
        if (this.ratingsByMovie.containsKey(rating.getMovieId())) {
            final Set<UserRating> ratingSet = this.ratingsByMovie.get(rating.getMovieId());
            ratingSet.add(rating);
        } else {
            final Set<UserRating> ratingSet = new HashSet<UserRating>();
            ratingSet.add(rating);
            this.ratingsByMovie.put(rating.getMovieId(), ratingSet);
        }

        // deal with adding this to the dataset per-user
        if (this.ratingsByUser.containsKey(rating.getUserId())) {
            final Map<MovieId, UserRating> ratingMap = this.ratingsByUser.get(rating.getUserId());
            ratingMap.put(rating.getMovieId(), rating);

            final Set<MovieId> movieSet = this.moviesByUser.get(rating.getUserId());
            movieSet.add(rating.getMovieId());
        } else {
            final Map<MovieId, UserRating> ratingMap = new HashMap<MovieId, UserRating>();
            ratingMap.put(rating.getMovieId(), rating);
            this.ratingsByUser.put(rating.getUserId(), ratingMap);

            final Set<MovieId> movieSet = new HashSet<MovieId>();
            movieSet.add(rating.getMovieId());
            this.moviesByUser.put(rating.getUserId(), movieSet);
        }

        // add to the max id, if necessary
        if (rating.getUserId().getValue() > this.maxUserId) {
            this.maxUserId = rating.getUserId().getValue();
        }
    }

    //
    // Deals with calculating an average rating value for a single user
    //

    public void calculateUserAverages() {
        this.averagesRowCount = this.ratingsByUser.keySet().size();
        for (final UserId user : this.ratingsByUser.keySet()) {
            this.averageUserVotes.put(user, this.calculateAverageUserVote(user));
            this.averagesRowStatus++;
        }
    }

    private Double calculateAverageUserVote(final UserId userId) {
        final Map<MovieId, UserRating> ratingSet = this.ratingsByUser.get(userId);
        if (ratingSet != null) {
            Double sum = 0.0;
            int ratingCount = 0;
            for (final UserRating rating : ratingSet.values()) {
                sum += rating.getRating();
                ratingCount++;
            }
            return sum / ratingCount;
        }
        return 0.0;
    }

    private Double getUserAverage(final UserId userId) {
        if (this.averageUserVotes.containsKey(userId)) {
            return this.averageUserVotes.get(userId);
        } else {
            // what to do if the user has no other votes?
            // setting them to true average
            return 3.0;
        }
    }

    //
    // Deal with calculating weights between two user ids
    //

    public Double calculateUserPairWeight(final UserId activeUser, final UserId otherUser) {
        // we need to sum up three values: activeDiff * otherDiff, activeDiff ^ 2, otherDiff ^ 2
        Double sumNumerator = 0.0;
        Double sumActiveDiffSquared = 0.0;
        Double sumOtherDiffSquared = 0.0;

        // get both of their averages
        final Double activeAverage = this.getUserAverage(activeUser);
        final Double otherAverage = this.getUserAverage(otherUser);

        final Set<MovieId> userRatingIntersection = new HashSet<MovieId>(
                this.moviesByUser.getOrDefault(activeUser, new HashSet<MovieId>()));
        userRatingIntersection.retainAll( this.moviesByUser.getOrDefault(otherUser, new HashSet<MovieId>()) );

        for (final MovieId movieId : userRatingIntersection) {
            // for every movie the active user rated, if the other user also rated it
            final UserRating activeRating = this.ratingsByUser.get(activeUser).get(movieId);
            final UserRating otherRating = this.ratingsByUser.get(otherUser).get(movieId);

            // calculate the active users difference from their average
            final Double activeDiff = activeRating.getRating() - activeAverage;
            final Double otherDiff = otherRating.getRating() - otherAverage;

            sumNumerator += (activeDiff * otherDiff);
            sumActiveDiffSquared += (activeDiff * activeDiff);
            sumOtherDiffSquared += (otherDiff * otherDiff);
        }

        final Double weight = sumNumerator / Math.sqrt( sumActiveDiffSquared * sumOtherDiffSquared );

        if (weight.isNaN()) {
            return 0.0; // ignore cases where the user always rates the same
        } else if (weight < 0.0) {
            return 0.0;
        }
        return weight;
    }

    //
    // Generic getters for various statuses
    //

    public boolean isComplete() {
        return this.completionStatus;
    }

    public int getAveragesRowCount() {
        return this.averagesRowCount;
    }

    public int getAveragesRowStatus() {
        return this.averagesRowStatus;
    }

    public AccuracyMeasurement getAccuracy() {
        return this.accuracy;
    }

    public int getMaxUserId() {
        return this.maxUserId;
    }
}