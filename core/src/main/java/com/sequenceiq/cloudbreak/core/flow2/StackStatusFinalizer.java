package com.sequenceiq.cloudbreak.core.flow2;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;

@Component
public class StackStatusFinalizer extends FlowFinalizerCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusFinalizer.class);

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    protected void doFinalize(Long resourceId) {
        stackStatusService.findFirstByStackIdOrderByCreatedDesc(resourceId)
                .filter(stackStatus -> stackStatus.getStatus() != null)
                .filter(stackStatus -> stackStatus.getStatus().isInProgress())
                .ifPresent(stackStatus -> {
                    Status finalStatus = stackStatus.getStatus().mapToFailedIfInProgress();
                    LOGGER.error("Flow completed with stack in {} status which is an in progress status. Mapping it to {} final state.",
                            stackStatus.getStatus(), finalStatus);
                    stackUpdater.updateStackStatusAndSetDetailedStatusToUnknown(resourceId, finalStatus);
                });
    }
}
