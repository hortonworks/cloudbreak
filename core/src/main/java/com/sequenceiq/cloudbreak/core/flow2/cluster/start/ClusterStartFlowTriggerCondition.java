package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterStartFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        ClusterView clusterView = stackView.getClusterView();
        boolean result = clusterView != null && clusterView.isStartRequested();
        if (!result) {
            LOGGER.warn("Cluster start cannot be triggered, because cluster {}", clusterView == null ? "is null" : "not in startRequested status");
        }
        return result;
    }
}
