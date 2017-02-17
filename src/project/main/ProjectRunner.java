package project.main;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
import project.main.runnable.UpdateTestingProgressRunnable;
import project.model.CollaborativeFilter;
import project.model.accuracy.AccuracyMeasurement;
import project.model.identifiers.MovieId;
import project.model.identifiers.UserId;
import project.model.netflix.UserRating;

public class ProjectRunner {

    private static final String FILE_TRAINING_FLAG = "i";
    private static final String FILE_TEST_FLAG = "t";
    private static final String USE_TRAINING_TESTING_FLAG = "ui";
    private static final String USE_TEST_TESTING_FLAG = "ut";
    private static final String DUMP_DATA_FLAG = "d";

    private static final String TRAINING_DATA = "Training";
    private static final String TEST_DATA = "Test";

    public static void main(String[] args) {
        try {
            // parse input args
            final CommandLine cmd = parseArgs(args);

            // verify inputs
            verifyInputs(cmd);

            // parse the input file
            final List<UserRating> data = parseInputData(cmd, TRAINING_DATA);

            // dump input data, if required
            dumpInputData(cmd, data);

            // learn
            final CollaborativeFilter filter = learnOnInputData(cmd, data);

            // test on training data
            if (cmd.hasOption(USE_TRAINING_TESTING_FLAG)) {
                testData(cmd, data, filter, "Training");
            }

            // test on test data
            if (cmd.hasOption(USE_TEST_TESTING_FLAG)) {
                final List<UserRating> testData = parseInputData(cmd, TEST_DATA);
                testData(cmd, testData, filter, "Test");
            }
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
        options.addOption(FILE_TRAINING_FLAG, true, "the training input file to process");
        options.addOption(FILE_TEST_FLAG, true, "the testing input file");
        options.addOption(USE_TRAINING_TESTING_FLAG, false, "signals the use of training data for accuracy measurement");
        options.addOption(USE_TEST_TESTING_FLAG, false, "signals the use of test data for accuracy measurement");
        options.addOption(DUMP_DATA_FLAG, false, "the input file to process");
        return options;
    }

    private static CommandLineParser initCommandParser() {
        return new DefaultParser();
    }

    private static void verifyInputs(final CommandLine cmd) {
        // make sure a file is passed in
        if (!cmd.hasOption(FILE_TRAINING_FLAG)) {
            throw new IllegalArgumentException("No training file provided.");
        }
    }

    private static List<UserRating> parseInputData(final CommandLine cmd, final String dataType)
        throws Exception
    {
        final String inputFileLoc;
        switch(dataType) {
        case TRAINING_DATA:
            inputFileLoc = cmd.getOptionValue(FILE_TRAINING_FLAG);
            System.out.println(String.format("Parsing training file {%s}.", inputFileLoc));
            break;
        case TEST_DATA:
            inputFileLoc = cmd.getOptionValue(FILE_TEST_FLAG);
            System.out.println(String.format("Parsing test file {%s}.", inputFileLoc));
            break;
        default:
            throw new IllegalArgumentException("Invalid data load found.");
        }

        final List<UserRating> records = new ArrayList<UserRating>();
        final File inputFile = new File(inputFileLoc);
        final CSVParser parser = CSVParser.parse(inputFile, Charset.forName("UTF-8"), CSVFormat.DEFAULT);
        for (final CSVRecord csvRecord : parser) {
            final MovieId movieId = MovieId.valueOf( Integer.parseInt( csvRecord.get(0) ) );
            final UserId userId = UserId.valueOf( Integer.parseInt( csvRecord.get(1) ) );
            final Double rating = Double.parseDouble( csvRecord.get(2) );
            records.add( new UserRating( userId, movieId, rating ) );
        }
        System.out.println(String.format("File {%s} parsed successfully.", inputFileLoc));
        return records;
    }

    private static void dumpInputData(final CommandLine cmd, final List<UserRating> data) {
        if (cmd.hasOption(DUMP_DATA_FLAG)) {
            System.out.println("Dumping top 50 records:");
            for (int i = 0; i < 50; i++) {
                final UserRating record = data.get(i);
                if (record != null) {
                    System.out.println(record);
                } else {
                    break;
                }
            }
        }
    }

    private static CollaborativeFilter learnOnInputData(final CommandLine cmd, final List<UserRating> data)
    {
        final CollaborativeFilterLearner learner = new NetflixCollaborativeLearner(data);
        (new Thread(new UpdateLearningProgressRunnable(learner))).start();
        final CollaborativeFilter filter = learner.learn();
        System.out.println("Collaborative filter learned successfully.");
        return filter;
    }

    private static void testData(final CommandLine cmd,
                                 final List<UserRating> data,
                                 final CollaborativeFilter filter,
                                 final String datasetName) {
        System.out.println("Testing accuracy of collaborative filter.");
        (new Thread(new UpdateTestingProgressRunnable(filter))).start();
        final AccuracyMeasurement measurement = filter.test(data, datasetName, 10);
        System.out.println(String.format("Accuracy for %s:", measurement.getName()));

        System.out.println(String.format("\tTested values: %d", measurement.getSampleCount()));
        System.out.println(String.format("\tMean absolute error: %1$.2f", measurement.getMeanAbsoluteError()));
        System.out.println(String.format("\tRoot mean squared error: %1$.2f", measurement.getRootMeanSquaredError()));
    }
}
