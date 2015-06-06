package com.sequenceiq.cloudbreak.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater;

@Service
@ConditionalOnExpression("${cb.stack.statuscheck.enabled:false}")
public class ScheduledStackStatusUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledStackStatusUpdaterService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private AmbariClusterStatusUpdater clusterStatusUpdater;

    @Scheduled(fixedDelayString = "${cb.stack.statuscheck.delay:300000}")
    public void updateStackStasuses() throws CloudbreakSecuritySetupException {
        Iterable<Stack> stacks = stackRepository.findAll();
        for (Stack stack : stacks) {
            clusterStatusUpdater.updateClusterStatus(stack);
        }
    }
}
