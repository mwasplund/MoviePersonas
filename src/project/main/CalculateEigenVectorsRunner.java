package project.main;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealVector;

public class CalculateEigenVectorsRunner {

    private static final String SIMILARITY_MATRIX_FILE_FLAG = "s";
    private static final String DEGREE_VECTOR_FILE_FLAG = "d";
    private static final String EIGENVECTOR_COUNT_FLAG = "k";

    public static void main(String[] args) {
        try {
            // parse input args
            final CommandLine cmd = parseArgs(args);

            // verify inputs
            verifyInputs(cmd);

            // get the count of eigenvectors
            final int kCount = 0; // TODO

            // parse the input file
            final EigenDecomposition decomposition = parseInputData(cmd);

            // output a new file with the enhanced genre data
            outputEigenvectors(decomposition, kCount);

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
        options.addOption(DEGREE_VECTOR_FILE_FLAG, true, "");
        options.addOption(EIGENVECTOR_COUNT_FLAG, true, "");
        return options;
    }

    private static CommandLineParser initCommandParser() {
        return new DefaultParser();
    }

    private static void verifyInputs(final CommandLine cmd) {
        // make sure a file is passed in
        if (!cmd.hasOption(SIMILARITY_MATRIX_FILE_FLAG)) {
            throw new IllegalArgumentException("No training input file provided.");
        }
        if (!cmd.hasOption(DEGREE_VECTOR_FILE_FLAG)) {
            throw new IllegalArgumentException("No training input file provided.");
        }
        if (!cmd.hasOption(EIGENVECTOR_COUNT_FLAG)) {
            throw new IllegalArgumentException("No training input file provided.");
        }
    }

    private static EigenDecomposition parseInputData(final CommandLine cmd)
        throws Exception
    {
        final String inputFileLoc = cmd.getOptionValue(SIMILARITY_MATRIX_FILE_FLAG);
        System.out.println(String.format("Reading {%s}.", inputFileLoc));

        final EigenDecomposition decomposition = new EigenDecomposition(null, null); // TODO

        return decomposition;
    }

    private static void outputEigenvectors(final EigenDecomposition decomposition,
                                           final int kCount)
        throws Exception
    {
        final String filename = String.format("netflix_data/eigenvectors-%d.txt", kCount);
        final Path filepath = Files.createFile(Paths.get(filename));
        final BufferedWriter writer = Files.newBufferedWriter(filepath);

        for (int i = 0; i < kCount; i++) {
            final RealVector eigenvector = decomposition.getEigenvector(i);
            final double[] eigenArray = eigenvector.toArray();
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < eigenArray.length; j++) {
                builder = builder.append(String.format("0.4f", eigenArray[j]));
                if (j < eigenArray.length - 1) {
                    builder = builder.append(",");
                }
            }
            writer.write(String.format("%s\n", builder.toString()));
        }

        System.out.println(String.format("Wrote output file {%s} successfully.", filename));
    }
}
