package project.model;

import java.util.ArrayDeque;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import project.model.accuracy.AccuracyMeasurement;
import project.model.identifiers.MovieId;
import project.model.identifiers.UserId;
import project.model.netflix.UserRating;

public class CollaborativeFilter {

    private final Map<MovieId, Set<UserRating>> ratingsByMovie;
    private final Map<UserId, Map<MovieId, UserRating>> ratingsByUser;
    private final Map<UserId, Set<MovieId>> moviesByUser;
    private final Map<UserId, Double> averageUserVotes;

    private int averagesRowCount = 0;
    private int averagesRowStatus = 0;
    private int testRowCount = 0;
    private int testRowStatus = 0;
    private boolean completionStatus = false;

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

    private Double calculateUserPairWeight(final UserId activeUser, final UserId otherUser) {
        // we need to sum up three values: activeDiff * otherDiff, activeDiff ^ 2, otherDiff ^ 2
        Double sumNumerator = 0.0;
        Double sumActiveDiffSquared = 0.0;
        Double sumOtherDiffSquared = 0.0;

        // get both of their averages
        final Double activeAverage = this.getUserAverage(activeUser);
        final Double otherAverage = this.getUserAverage(otherUser);

        final Set<MovieId> userRatingIntersection = new HashSet<MovieId>(this.moviesByUser.get(activeUser));
        userRatingIntersection.retainAll( this.moviesByUser.get(otherUser) );

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
        }
        return weight;
    }

    //
    // User for predicting the rating a user will have for a single movie
    //

    public Double predictValue(final UserId userId,
                               final MovieId movieId) {
        final Double activeUserAverage = this.getUserAverage(userId);

        if (this.ratingsByMovie.containsKey(movieId)) {
            Double weightSum = 0.0;
            Double filterSum = 0.0;
            for (final UserRating rating : this.ratingsByMovie.get(movieId)) {
                final Double weight = this.calculateUserPairWeight(userId, rating.getUserId());
                final Double ithUserAverage = this.getUserAverage(rating.getUserId());
                final Double userFilter = weight * ( rating.getRating() - ithUserAverage );

                weightSum += Math.abs(weight);
                filterSum += userFilter;
            }
            final Double kappa = 1.0 / weightSum;
            if (Double.valueOf(activeUserAverage + ( kappa * filterSum )).isNaN()) {
                return activeUserAverage; // when there is no weighting for this pair, just return the average
            }
            return activeUserAverage + ( kappa * filterSum );
        } else {
            // if no one else has voted on this movie, just guess with the user average
            return activeUserAverage;
        }
    }

    //
    // Used for testing a large dataset against the predicted ratings
    //

    public AccuracyMeasurement test(final List<UserRating> testRatings,
                                    final String testName,
                                    final int threadCount) {
        this.testRatings = new ArrayDeque<UserRating>(testRatings);
        this.testRowCount = testRatings.size();

        this.accuracy = new AccuracyMeasurement(testName);

        final ExecutorService es = Executors.newCachedThreadPool();
        final int cores = Runtime.getRuntime().availableProcessors();
        System.out.println(String.format("Testing with %d threads.", cores));
        for(int i = 0; i < cores; i++) {
            es.execute(new TestFilterRunnable(this, this.accuracy, "test-" + i));
        }
        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (final InterruptedException ie) { ; }

        this.accuracy.calculateAccuracy();
        this.completionStatus = true;
        return this.accuracy;
    }

    private ArrayDeque<UserRating> testRatings;

    private synchronized UserRating getNextRating() {
        return this.testRatings.poll();
    }

    private class TestFilterRunnable implements Runnable {

        final CollaborativeFilter filter;
        final AccuracyMeasurement accuracy;
        final String threadId;

        public TestFilterRunnable(final CollaborativeFilter filter,
                                  final AccuracyMeasurement accuracy,
                                  final String threadId) {
            this.filter = filter;
            this.accuracy = accuracy;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            try {
                UserRating rating = this.filter.getNextRating();
                while (rating != null) {
                    final Double error = this.filter.predictValue(rating.getUserId(), rating.getMovieId()) - rating.getRating();
                    this.accuracy.addRawError(error);
                    this.filter.testRowStatus++;
                    rating = this.filter.getNextRating();
                }
            } catch (final EmptyStackException ese) { ; }
        }
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

    public int getTestRowCount() {
        return this.testRowCount;
    }

    public int getTestRowStatus() {
        return this.testRowStatus;
    }

    public AccuracyMeasurement getAccuracy() {
        return this.accuracy;
    }
}