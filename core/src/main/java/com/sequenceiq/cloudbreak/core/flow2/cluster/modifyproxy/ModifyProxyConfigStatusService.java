package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_ON_CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_SALT_STATE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_SUCCESS;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action.ModifyProxyConfigFailedAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class ModifyProxyConfigStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigFailedAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void applyingSaltState(Long stackId) {
        LOGGER.info("Start modify proxy config salt state apply");
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.MODIFY_PROXY_CONFIG_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_MODIFY_PROXY_CONFIG_SALT_STATE);
    }

    public void updateClusterManager(Long stackId) {
        LOGGER.info("Updating proxy config settings on CM");
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_MODIFY_PROXY_CONFIG_ON_CM);
    }

    public void success(Long stackId) {
        LOGGER.info("Finished modify proxy config state");
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CLUSTER_MODIFY_PROXY_CONFIG_SUCCESS);
    }

    public void failed(Long stackId, Exception exception) {
        LOGGER.error("Failed modify proxy config state", exception);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.SALT_UPDATE_FAILED, exception.getMessage());
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), CLUSTER_MODIFY_PROXY_CONFIG_FAILED, exception.getMessage());
    }
}
