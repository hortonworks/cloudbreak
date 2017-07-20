package com.sequenceiq.cloudbreak.service.ha;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

/**
 * InMemoryStateStore is for storing the cloudbreak flow statuses.
 * In HA mode this service removing the entries which are under deletion on other nodes.
 * The deletion flows should not check the cancel criteria because then we cancel
 * the termination flow on the node where that is running.
 */
@Service
public class InMemoryStateStoreCleanupService {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    public void cleanupStacksWhichAreDeleteInProgressOnOtherCloudbreakNodes() {
        List<Stack> allWhereDeleteInProgress = stackRepository.findAllWhereDeleteInProgress();
        for (Stack stack : allWhereDeleteInProgress) {
            PollGroup pollGroup = InMemoryStateStore.getStack(stack.getId());
            if (pollGroup != null) {
                InMemoryStateStore.putStack(stack.getId(), PollGroup.CANCELLED);
            }
        }
    }

    public void cleanupStackWhichAreDeleteInProgressOnOtherCloudbreakNodes(Long stackId) throws CloudbreakOrchestratorCancelledException {
        Set<FlowLog> stackFlows = flowLogRepository.findAllTerminationFlowByStackId(stackId);
        if (cloudbreakNodeConfig.isNodeIdSpecified()) {
            for (FlowLog stackFlow : stackFlows) {
                if (!cloudbreakNodeConfig.getId().equals(stackFlow.getCloudbreakNodeId())) {
                    throw new CancellationException(
                            String.format("The %s flow must cancel because there is a termination action on %s node: %s",
                                    stackFlow, stackFlow.getCloudbreakNodeId(), stackFlow.toString()));
                }
            }
        }
    }
}
