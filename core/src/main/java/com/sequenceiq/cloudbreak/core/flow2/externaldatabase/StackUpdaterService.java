package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class StackUpdaterService {

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    public void updateStatus(Long stackId, DetailedStackStatus detailedStackStatus, ResourceEvent resourceEvent, String statusReason) {
        stackUpdater.updateStackStatus(stackId, detailedStackStatus, statusReason);
        flowMessageService.fireEventAndLog(stackId, detailedStackStatus.getStatus().name(), resourceEvent);

    }
}
