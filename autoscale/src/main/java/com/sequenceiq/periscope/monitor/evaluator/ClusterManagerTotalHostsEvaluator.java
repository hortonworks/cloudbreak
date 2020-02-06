package com.sequenceiq.periscope.monitor.evaluator;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManagerVariant;

public interface ClusterManagerTotalHostsEvaluator {
    ClusterManagerVariant getSupportedClusterManagerVariant();

    int getTotalHosts(Cluster cluster);
}
