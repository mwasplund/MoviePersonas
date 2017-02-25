package project.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import project.model.identifiers.ClusterId;
import project.model.identifiers.UserId;

public class ClusteredFilter extends CollaborativeFilter {

    protected final Map<ClusterId, Set<UserId>> userClusters;

    public ClusteredFilter() {
        this.userClusters = new HashMap<ClusterId, Set<UserId>>();
    }
}