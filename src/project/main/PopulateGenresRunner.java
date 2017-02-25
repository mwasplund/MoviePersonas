package project.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import project.model.identifiers.Genre;
import project.model.identifiers.MovieId;
import project.model.identifiers.MovieName;
import project.model.movie.Movie;

public class PopulateGenresRunner {

    private static final String MOVIE_TITLES_FILE_FLAG = "f";

    public static void main(String[] args) {
        try {
            // parse input args
            final CommandLine cmd = parseArgs(args);

            // verify inputs
            verifyInputs(cmd);

            // parse the input file
            final List<Movie> data = parseInputData(cmd);

            // output a new file with the enhanced genre data
            outputMovieGenres(data);

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
        options.addOption(MOVIE_TITLES_FILE_FLAG, true, "the movie input file to process");
        return options;
    }

    private static CommandLineParser initCommandParser() {
        return new DefaultParser();
    }

    private static void verifyInputs(final CommandLine cmd) {
        // make sure a file is passed in
        if (!cmd.hasOption(MOVIE_TITLES_FILE_FLAG)) {
            throw new IllegalArgumentException("No movie input file provided.");
        }
    }

    private static List<Movie> parseInputData(final CommandLine cmd)
        throws Exception
    {
        final String inputFileLoc = cmd.getOptionValue(MOVIE_TITLES_FILE_FLAG);
        System.out.println(String.format("Reading {%s}.", inputFileLoc));

        final List<Movie> movies = new ArrayList<Movie>();
        final File inputFile = new File(inputFileLoc);
        final CSVParser parser = CSVParser.parse(inputFile, Charset.forName("UTF-8"), CSVFormat.DEFAULT);
        int count = 1;
        for (final CSVRecord csvRecord : parser) {
            final MovieId movieId = MovieId.valueOf( Integer.parseInt( csvRecord.get(0) ) );
            final MovieName movieName = MovieName.valueOf( csvRecord.get(2) );
            final List<Genre> genres = parseGenres(movieName);
            final Movie movie = new Movie(movieId, movieName);
            for (final Genre genre : genres) {
                movie.withGenre(genre);
            }
            movies.add(movie);
            if (count % 100 == 0) {
                System.out.println(String.format("Enhanced %d movies.", count));
            }
            count++;
        }
        System.out.println(String.format("File {%s} enhanced successfully.", inputFileLoc));
        return movies;
    }

    private static List<Genre> parseGenres(final MovieName movieName)
        throws Exception
    {
        final List<Genre> genres = new ArrayList<Genre>();

        CloseableHttpResponse response = null;
        try {
            final CloseableHttpClient httpclient = HttpClients.createDefault();
            final HttpGet httpGet =
                    new HttpGet(
                            String.format("http://www.omdbapi.com/?t=%s&y=&plot=short&r=json",
                                          URLEncoder.encode(movieName.getValue().replace(" ", "+"), "UTF-8")));
            response = httpclient.execute(httpGet);

            // get the response from omdb
            final HttpEntity entity = response.getEntity();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            final StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            reader.close();

            // read in the json
            final JSONObject obj = new JSONObject(out.toString());

            // get the genre list, if the movie exists
            final String responseString = obj.getString("Response");
            if (responseString == null || !responseString.equals("False")) {
                final String genreStringList = obj.getString("Genre");
                final String[] genreStringArray = genreStringList.split(",");
                for (final String genreString : genreStringArray) {
                    genres.add(Genre.valueOf(genreString));
                }
            } else {
                genres.add(Genre.valueOf("N/A"));
            }

            // consume the entity
            EntityUtils.consume(entity);
        } catch (final Exception e) {
            System.out.println(String.format("Failed on %s.", movieName.getValue()));
            genres.add(Genre.valueOf("N/A"));
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return genres;
    }

    private static void outputMovieGenres(final List<Movie> movies)
        throws Exception
    {
        final PrintWriter pw = new PrintWriter(new FileWriter("netflix_data/enhance_movie_data.txt"));

        for (final Movie movie : movies) {
            for (final Genre genre : movie.getGenres()) {
                pw.write(String.format("%d,%s,%s\n", movie.getId().getValue(), movie.getName().getValue(), genre.getValue()));
            }
        }

        pw.close();
        System.out.println("Wrote output file {netflix_data/enhance_movie_data.txt} successfully.");
    }
}
