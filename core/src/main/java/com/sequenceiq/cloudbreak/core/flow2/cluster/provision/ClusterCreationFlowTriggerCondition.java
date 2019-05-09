package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterCreationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        boolean result = stackView.isAvailable() && stackView.getClusterView() != null && stackView.getClusterView().isRequested();
        if (!result) {
            LOGGER.debug("Cluster creation cannot be triggered, because cluster is not in requested status or stack is not available.");
        }
        return result;
    }
}
