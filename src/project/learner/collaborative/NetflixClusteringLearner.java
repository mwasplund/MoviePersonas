package project.learner.collaborative;

import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import project.model.ClusteredFilter;
import project.model.identifiers.KValue;
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

        final double[] diagonalMatrix = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                diagonalMatrix[i] += adjacencyMatrix[i][j];
            }
            diagonalMatrix[i] = 1.0 / Math.sqrt(diagonalMatrix[i]);
        }

        final double[][] lMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                lMatrix[i][j] = diagonalMatrix[i] * adjacencyMatrix[i][j] * diagonalMatrix[j];
                lMatrix[j][i] = lMatrix[i][j];
            }
        }

        final RealMatrix lRealMatrix = new Array2DRowRealMatrix(lMatrix);
        final EigenDecomposition decomposition = new EigenDecomposition(lRealMatrix);

        //final double[][] eigenvectors = eigen.getEigenVectors();
        //for (int i = 0; i < n; i++) {
        //    Math.unitize2(Y[i]);
        //}

        /*
         * TODO

        final KMeans kmeans = new KMeans(Y, k);
        distortion = kmeans.distortion;
        y = kmeans.getClusterLabel();
        size = kmeans.getClusterSize();
        */

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