package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component("ClusterTerminationFailureAction")
public class ClusterTerminationFailureAction extends AbstractClusterTerminationAction<TerminateClusterResult> {

    protected ClusterTerminationFailureAction() {
        super(TerminateClusterResult.class);
    }

    @Override
    protected void doExecute(ClusterContext context, TerminateClusterResult payload, Map<Object, Object> variables) throws Exception {

    }

    @Override
    protected Long getClusterId(TerminateClusterResult payload) {
        return null;
    }
}
