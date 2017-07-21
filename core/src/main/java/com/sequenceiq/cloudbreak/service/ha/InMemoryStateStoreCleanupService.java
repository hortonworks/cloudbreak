package com.sequenceiq.cloudbreak.service.ha;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
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
        for (Long stackId : stackRepository.findAllStackIdByStatus(Status.DELETE_IN_PROGRESS)) {
            PollGroup pollGroup = InMemoryStateStore.getStack(stackId);
            if (pollGroup != null) {
                InMemoryStateStore.putStack(stackId, PollGroup.CANCELLED);
            }
        }
    }
}
