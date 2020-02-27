package com.sequenceiq.periscope.monitor.evaluator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;

public abstract class ClusterCreationEvaluator extends EvaluatorExecutor {
    public abstract ClusterManagerVariant getSupportedClusterManagerVariant();
}
