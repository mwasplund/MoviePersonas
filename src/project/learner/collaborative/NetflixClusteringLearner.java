package project.learner.collaborative;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import project.model.ClusteredFilter;
import project.model.ReducedUserProfile;
import project.model.identifiers.KValue;
import project.model.identifiers.UserId;
import project.model.netflix.UserRating;

public class NetflixClusteringLearner extends NetflixCollaborativeLearner {

    private ClusteredFilter currentFilter;
    private final double[][] adjacencyMatrix;
    private final KValue kValue;

    public NetflixClusteringLearner(final List<UserRating> records,
                                    final double[][] adjacencyMatrix,
                                    final KValue kValue) {
        super(records);
        this.adjacencyMatrix = adjacencyMatrix;
        this.kValue = kValue;
    }

    @Override
    public ClusteredFilter learn() {
        this.state = "LOADING";
        this.currentFilter = new ClusteredFilter();
        for (final UserRating record : this.records) {
            this.currentFilter.addRating( record );
            this.rowsProcessed++;
        }
        System.out.println("Calculating user average ratings.");

        this.state = "AVERAGES";
        this.currentFilter.calculateUserAverages();
        System.out.println(String.format(
                "User average ratings calculated successfully (%d of %d).",
                this.currentFilter.getAveragesRowStatus(),
                this.currentFilter.getAveragesRowCount()));

        this.completionStatus = true;

        // perform spectral filtering
        this.currentFilter = this.spectralCluster(this.adjacencyMatrix, this.kValue.getValue());
        return this.currentFilter;
    }

    public ClusteredFilter spectralCluster(final double[][] adjacencyMatrix,
                                           final int k) {
        final ClusteredFilter filter = this.currentFilter;
        final int n = adjacencyMatrix.length;
        System.out.println(String.format("A[%d][%d] = %.3f", n, n, adjacencyMatrix[n-1][n-1]));

        System.out.println(String.format("Building diagonal matrix."));
        final double[] diagonalMatrix = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                diagonalMatrix[i] += adjacencyMatrix[i][j];
            }
            diagonalMatrix[i] = 1.0 / Math.sqrt(diagonalMatrix[i]);
        }
        System.out.println(String.format("Built diagonal matrix of dimensions %dx%d.", n, n));
        System.out.println(String.format("D[%d][%d] = %.3f", n, n, diagonalMatrix[n-1]));

        System.out.println(String.format("Building L matrix."));
        final double[][] lMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                lMatrix[i][j] = diagonalMatrix[i] * adjacencyMatrix[i][j] * diagonalMatrix[j];
                lMatrix[j][i] = lMatrix[i][j];
            }
        }
        System.out.println(String.format("Built L matrix of dimensions %dx%d.", n, n));

        System.out.println(String.format("Decomposing L matrix."));
        final RealMatrix lRealMatrix = new Array2DRowRealMatrix(lMatrix);
        System.out.println(String.format("L[%d][%d] = %.3f", n, n, lRealMatrix.getEntry(n-1, n-1)));
        final EigenDecomposition decomposition = new EigenDecomposition(lRealMatrix);
        System.out.println(String.format("Decomposed L matrix."));

        // get the first k eigenvectors
        System.out.println(String.format("Identifiying eigenvectors."));
        double[][] eigenvectors = new double[n][k];
        for (int i = 0; i < k; i++) {
            final RealVector vector = decomposition.getEigenvector(i);
            for (int j = 0; j < n; j++) {
                eigenvectors[j][i] = vector.getEntry(j);
            }
        }
        final RealMatrix eigenMatrix = new Array2DRowRealMatrix(eigenvectors);
        System.out.println(String.format("Identified %d eigenvectors.", k));

        // calculate the matrix Z
        System.out.println(String.format("Reducing dimensions of L->Z."));
        final RealMatrix zMatrix = lRealMatrix.multiply(eigenMatrix);
        System.out.println(String.format("Reduced dimensions to %dx%d", n, k));

        System.out.println(String.format("Building user profiles."));
        final List<ReducedUserProfile> userProfiles = new ArrayList<ReducedUserProfile>();
        for (int i = 0; i < n; i++) {
            double[] reducedPoints = new double[k];
            for (int j = 0; j < k; j++) {
                reducedPoints[j] = zMatrix.getEntry(i, j);
            }
            userProfiles.add(new ReducedUserProfile(UserId.valueOf(i), reducedPoints));
        }
        System.out.println(String.format("Built %d profiles.", n));

        // cluster the user profiles
        System.out.println(String.format("Clustering users."));
        final KMeansPlusPlusClusterer clusterer =
                new KMeansPlusPlusClusterer<ReducedUserProfile>(this.kValue.getValue(), 100);
        filter.setCentroids(clusterer.cluster(userProfiles));
        System.out.println(String.format("Finished clustering users."));

        return filter;
    }

    @Override
    public String getStatus() {
        String retString = "";
        switch (this.state) {
        case "INIT":
            retString = "Learning not yet started";
            break;
        case "LOADING":
            retString = String.format("Loading input data, %d of %d (%.2f%%) complete",
                                this.rowsProcessed,
                                this.totalRowCount,
                                (100.0 * this.rowsProcessed / this.totalRowCount));
            break;
        case "AVERAGES":
            retString = String.format("Calculating user average values, %d of %d (%.2f%%) complete",
                                this.currentFilter.getAveragesRowStatus(),
                                this.currentFilter.getAveragesRowCount(),
                                (100.0 * this.currentFilter.getAveragesRowStatus() / this.currentFilter.getAveragesRowCount()));
            break;
        default:
            retString = "Complete";
        }

        return retString;
    }
}