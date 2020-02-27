package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManagerVariant;

public interface ClusterManagerSpecificHostHealthEvaluator {

    ClusterManagerVariant getSupportedClusterManagerVariant();

    List<String> determineHostnamesToRecover(Cluster cluster);
}
