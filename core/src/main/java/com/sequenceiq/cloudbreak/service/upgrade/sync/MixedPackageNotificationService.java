package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
class MixedPackageNotificationService {

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private StackStatusService stackStatusService;

    void sendNotification(Long stackId, ResourceEvent resourceEvent, List<String> args) {
        Optional<StackStatus> currentStatus = stackStatusService.findFirstByStackIdOrderByCreatedDesc(stackId);
        Status status = currentStatus.isPresent() && UPDATE_FAILED.equals(currentStatus.get().getStatus()) ? UPDATE_FAILED : UPDATE_IN_PROGRESS;
        eventService.fireCloudbreakEvent(stackId, status.name(), resourceEvent, args);
    }
}
