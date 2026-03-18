package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPDATE_TRUSTED_REALM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPDATE_TRUSTED_REALM_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class UpdateTrustedRealmStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmStatusService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void updatingTrustedRealm(Long stackId) {
        LOGGER.info("Updating trusted realm configuration on CM for stack {}", stackId);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPDATE_TRUSTED_REALM_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_UPDATE_TRUSTED_REALM);
    }

    public void success(Long stackId) {
        LOGGER.info("Trusted realm configuration update finished successfully for stack {}", stackId);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE);
    }

    public void failed(Long stackId, Exception exception) {
        LOGGER.error("Trusted realm configuration update failed for stack {}", stackId, exception);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPDATE_TRUSTED_REALM_FAILED, exception.getMessage());
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_UPDATE_TRUSTED_REALM_FAILED, exception.getMessage());
    }
}


