package project.main;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import project.learner.collaborative.CollaborativeFilterLearner;
import project.learner.collaborative.NetflixCollaborativeLearner;
import project.main.runnable.UpdateLearningProgressRunnable;
import project.model.CollaborativeFilter;
import project.model.identifiers.MovieId;
import project.model.identifiers.UserId;
import project.model.netflix.UserRating;

public class PopulateSimilarityMatrixRunner {

    private static final String TRAINING_RATINGS_FILE_FLAG = "f";
    private static final String USER_COUNT_FLAG = "u";

    public static void main(String[] args) {
        try {
            // parse input args
            final CommandLine cmd = parseArgs(args);

            // verify inputs
            verifyInputs(cmd);

            // parse the input file
            final CollaborativeFilter filter = parseInputData(cmd);

            // output a new file with the enhanced genre data
            outputSimilarityMatrix(filter);

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
        options.addOption(TRAINING_RATINGS_FILE_FLAG, true, "the movie input file to process");
        options.addOption(USER_COUNT_FLAG, true, "the number of users to process");
        return options;
    }

    private static CommandLineParser initCommandParser() {
        return new DefaultParser();
    }

    private static void verifyInputs(final CommandLine cmd) {
        // make sure a file is passed in
        if (!cmd.hasOption(TRAINING_RATINGS_FILE_FLAG)) {
            throw new IllegalArgumentException("No training input file provided.");
        }
        if (!cmd.hasOption(USER_COUNT_FLAG)) {
            throw new IllegalArgumentException("No user count provided.");
        }
    }

    private static CollaborativeFilter parseInputData(final CommandLine cmd)
        throws Exception
    {
        final String inputFileLoc = cmd.getOptionValue(TRAINING_RATINGS_FILE_FLAG);
        System.out.println(String.format("Reading {%s}.", inputFileLoc));

        final Integer userCount = Integer.parseInt(cmd.getOptionValue(USER_COUNT_FLAG));

        // load in the file
        final Map<UserId, Integer> userIdTruncationMap = new HashMap<UserId, Integer>();
        final List<UserRating> records = new ArrayList<UserRating>();
        final File inputFile = new File(inputFileLoc);
        final CSVParser parser = CSVParser.parse(inputFile, Charset.forName("UTF-8"), CSVFormat.DEFAULT);
        for (final CSVRecord csvRecord : parser) {
            final MovieId movieId = MovieId.valueOf( Integer.parseInt( csvRecord.get(0) ) );
            final UserId userId = UserId.valueOf( Integer.parseInt( csvRecord.get(1) ) );
            final Double rating = Double.parseDouble( csvRecord.get(2) );
            if (userIdTruncationMap.size() <= userCount || userIdTruncationMap.containsKey(userId)) {
                final Integer nextId = userIdTruncationMap.getOrDefault(userId, userIdTruncationMap.size());

                records.add(new UserRating(UserId.valueOf(nextId), movieId, rating));
                userIdTruncationMap.put(userId, nextId);
            }
        }
        System.out.println(String.format("File {%s} parsed successfully.", inputFileLoc));

        // learn on the data
        final CollaborativeFilterLearner learner = new NetflixCollaborativeLearner(records);
        (new Thread(new UpdateLearningProgressRunnable(learner))).start();
        final CollaborativeFilter filter = learner.learn();
        System.out.println("Collaborative filter learned successfully.");
        return filter;
    }

    private static void outputSimilarityMatrix(final CollaborativeFilter filter)
        throws Exception
    {
        final int maxUserId = filter.getMaxUserId();

        Path filepath = Paths.get(String.format("netflix_data/similarity-matrix-%d.txt", maxUserId));
        Files.deleteIfExists(filepath);
        filepath = Files.createFile(filepath);
        final BufferedWriter writer = Files.newBufferedWriter(filepath);

        final double[][] similarityMatrix = new double[maxUserId][maxUserId];
        final int[] degreeVector = new int[maxUserId];

        System.out.println(String.format("Starting to calculate similarity matrix."));

        for (int row = 0; row < maxUserId; row++) {
            int degree = 0;
            for (int col = 0; col < maxUserId; col++) {
                if (row <= col) {
                    similarityMatrix[row][col] =
                        filter.calculateUserPairWeight(
                            UserId.valueOf(row),
                            UserId.valueOf(col));
                } else {
                    // if the row is larger than the column, we already calculated
                    // this pair - just load that values
                    similarityMatrix[row][col] =
                            similarityMatrix[col][row];
                }

                // also calculate degree
                if (similarityMatrix[row][col] > 0.1) {
                    degree++;
                }
            }
            degreeVector[row] = degree;
            System.out.println(String.format("Calculated row %d successfully.", row));
        }

        for (int row = 0; row < maxUserId; row++) {
            final StringBuilder builder = new StringBuilder();
            for (int col = 0; col < maxUserId; col++) {
                builder.append(String.format("%.3f",similarityMatrix[row][col]));
                if (col < maxUserId - 1) {
                    builder.append(",");
                }
            }
            // output this row of the matrix
            writer.write(String.format("%s\n", builder.toString()));
        }

        writer.flush();
        writer.close();
        System.out.println(String.format("Wrote output file {%s} successfully.", filepath.getFileName().toString()));
    }
}
