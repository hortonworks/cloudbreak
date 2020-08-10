package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;

public interface ClusterManagerSpecificHostHealthEvaluator {

    ClusterManagerVariant getSupportedClusterManagerVariant();

    List<String> determineHostnamesToRecover(Cluster cluster);
}
