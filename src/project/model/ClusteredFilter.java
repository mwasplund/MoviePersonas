package project.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.ml.clustering.CentroidCluster;

import project.model.identifiers.ClusterId;
import project.model.identifiers.UserId;

public class ClusteredFilter extends CollaborativeFilter {

    protected final Map<ClusterId, Set<UserId>> userClusters;

    public ClusteredFilter() {
        this.userClusters = new HashMap<ClusterId, Set<UserId>>();
    }

    public void setCentroids(final List<CentroidCluster<ReducedUserProfile>> userProfiles) {
        int count = 1;
        for (final CentroidCluster<ReducedUserProfile> cluster : userProfiles) {
            final ClusterId clusterId = ClusterId.valueOf(count);
            final Set<UserId> userIds = new HashSet<UserId>();
            for (final ReducedUserProfile profile : cluster.getPoints()) {
                userIds.add(profile.getUserId());
            }
            this.userClusters.put(clusterId, userIds);
            count++;
        }
    }

    public void silhouette()
        throws Exception
    {
        for (final Entry<ClusterId, Set<UserId>> entry : this.userClusters.entrySet()) {
            // for each cluster, calculate two MAE's:
            // 1) one for each member of the same cluster
            // 2) one for members of other clusters

            int originalClusterId = entry.getKey().getValue();
            double runningCountSameCluster = 0.0;
            double runningCountOtherCluster = 0.0;
            double runningErrorSameCluster = 0.0;
            double runningErrorOtherCluster = 0.0;

            for (final UserId originalUserId : entry.getValue()) {
                for (final Entry<ClusterId, Set<UserId>> clusterEntry : this.userClusters.entrySet()) {
                    int newClusterId = clusterEntry.getKey().getValue();
                    if (newClusterId == originalClusterId) {
                        for (final UserId newUserId : clusterEntry.getValue()) {
                            runningCountSameCluster += 1.0;
                            runningErrorSameCluster += (1.0 - this.calculateUserPairWeight(originalUserId, newUserId));
                        }
                    } else {
                        for (final UserId newUserId : clusterEntry.getValue()) {
                            runningCountOtherCluster += 1.0;
                            runningErrorOtherCluster += (1.0 - this.calculateUserPairWeight(originalUserId, newUserId));
                        }
                    }
                }
            }

            System.out.println(String.format("Cluster %d. Same MAE: %.3f, other MAE: %.3f",
                                             entry.getKey().getValue(),
                                             runningErrorSameCluster / runningCountSameCluster,
                                             runningErrorOtherCluster / runningCountOtherCluster));
        }
    }

    public void print()
        throws Exception
    {
        for (final Entry<ClusterId, Set<UserId>> entry : this.userClusters.entrySet()) {
            System.out.println(String.format("Cluster %d:", entry.getKey().getValue()));
            for (final UserId userId : entry.getValue()) {
                System.out.println(String.format("\t%d", userId.getValue()));
            }
        }
    }
}