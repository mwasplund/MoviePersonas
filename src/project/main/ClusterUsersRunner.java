package project.main;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import project.learner.collaborative.NetflixClusteringLearner;
import project.model.ClusteredFilter;
import project.model.identifiers.KValue;
import project.model.identifiers.MovieId;
import project.model.identifiers.UserId;
import project.model.netflix.UserRating;

public class ClusterUsersRunner {

    private static final String SIMILARITY_MATRIX_FILE_FLAG = "s";
    private static final String TRAINING_RATINGS_FILE_FLAG = "f";
    private static final String EIGENVECTOR_COUNT_FLAG = "k";
    private static final String USER_COUNT_FLAG = "u";

    public static void main(String[] args) {
        try {
            // parse input args
            final CommandLine cmd = parseArgs(args);

            // verify inputs
            verifyInputs(cmd);

            // get the count of eigenvectors
            final KValue kCount = new KValue(Integer.parseInt(cmd.getOptionValue(EIGENVECTOR_COUNT_FLAG)));

            // parse the input files
            final List<UserRating> data = parseInputData(cmd);
            final double[][] similarityMatrix = parseAdjacencyMatrix(cmd);

            // run the clustering algorithms
            final ClusteredFilter filter = clusterUsers(data, similarityMatrix, kCount);

            analyzeSilhouette(filter);

            // output the clusters
            printClusters(filter);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static CommandLine parseArgs(final String[] args)
        throws ParseException
    {
        final Options options = initOptions();
        final CommandLineParser cmdParser = initCommandParser();
        return cmdParser.parse(options, args);
    }

    private static Options initOptions() {
        final Options options = new Options();
        options.addOption(SIMILARITY_MATRIX_FILE_FLAG, true, "");
        options.addOption(TRAINING_RATINGS_FILE_FLAG, true, "");
        options.addOption(EIGENVECTOR_COUNT_FLAG, true, "");
        options.addOption(USER_COUNT_FLAG, true, "the number of users to process");
        return options;
    }

    private static CommandLineParser initCommandParser() {
        return new DefaultParser();
    }

    private static void verifyInputs(final CommandLine cmd) {
        // make sure a file is passed in
        if (!cmd.hasOption(SIMILARITY_MATRIX_FILE_FLAG)) {
            throw new IllegalArgumentException("No similarity matrix file provided.");
        }
        if (!cmd.hasOption(TRAINING_RATINGS_FILE_FLAG)) {
            throw new IllegalArgumentException("No similarity matrix file provided.");
        }
        if (!cmd.hasOption(EIGENVECTOR_COUNT_FLAG)) {
            throw new IllegalArgumentException("No eigenvector count provided.");
        }
        if (!cmd.hasOption(USER_COUNT_FLAG)) {
            throw new IllegalArgumentException("No user count provided.");
        }
    }

    private static double[][] parseAdjacencyMatrix(final CommandLine cmd)
        throws Exception
    {
        final String inputFileLoc = cmd.getOptionValue(SIMILARITY_MATRIX_FILE_FLAG);
        System.out.println(String.format("Reading {%s}.", inputFileLoc));

        final Integer size = Integer.parseInt(cmd.getOptionValue(USER_COUNT_FLAG));
        double[][] similarityMatrix = new double[size][size];
        final File inputFile = new File(inputFileLoc);
        final CSVParser parser = CSVParser.parse(inputFile, Charset.forName("UTF-8"), CSVFormat.DEFAULT);

        int row = 0;
        for (final CSVRecord csvRecord : parser) {
            for (int i = 0; i < csvRecord.size(); i++) {
                similarityMatrix[row][i] = Double.parseDouble(csvRecord.get(i));
            }
            row++;
        }
        return similarityMatrix;
    }

    private static List<UserRating> parseInputData(final CommandLine cmd)
        throws Exception
    {
        final String inputFileLoc = cmd.getOptionValue(TRAINING_RATINGS_FILE_FLAG);
        System.out.println(String.format("Reading {%s}.", inputFileLoc));

        // load in the file
        final Integer size = Integer.parseInt(cmd.getOptionValue(USER_COUNT_FLAG));
        final Map<UserId, Integer> userIdTruncationMap = new HashMap<UserId, Integer>();
        final List<UserRating> records = new ArrayList<UserRating>();
        final File inputFile = new File(inputFileLoc);
        final CSVParser parser = CSVParser.parse(inputFile, Charset.forName("UTF-8"), CSVFormat.DEFAULT);
        for (final CSVRecord csvRecord : parser) {
            final MovieId movieId = MovieId.valueOf( Integer.parseInt( csvRecord.get(0) ) );
            final UserId userId = UserId.valueOf( Integer.parseInt( csvRecord.get(1) ) );
            final Double rating = Double.parseDouble( csvRecord.get(2) );
            if (userIdTruncationMap.size() < size || userIdTruncationMap.containsKey(userId)) {
                final Integer nextId = userIdTruncationMap.getOrDefault(userId, userIdTruncationMap.size());

                records.add(new UserRating(UserId.valueOf(nextId), movieId, rating));
                userIdTruncationMap.put(userId, nextId);
            }
        }
        System.out.println(String.format("File {%s} parsed successfully.", inputFileLoc));

        return records;
    }

    private static ClusteredFilter clusterUsers(final List<UserRating> data,
                                                final double[][] similarityMatrix,
                                                final KValue kCount)
        throws Exception
    {
        return new NetflixClusteringLearner(data, similarityMatrix, kCount).learn();
    }

    private static void analyzeSilhouette(final ClusteredFilter filter)
        throws Exception
    {
        filter.silhouette();
    }

    private static void printClusters(final ClusteredFilter filter)
        throws Exception
    {
        //filter.print();
    }
}
