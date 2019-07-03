package com.sequenceiq.periscope.monitor.evaluator;

import com.sequenceiq.periscope.domain.ClusterManagerVariant;

public abstract class ClusterCreationEvaluator extends EvaluatorExecutor {
    public abstract ClusterManagerVariant getSupportedClusterManagerVariant();
}
