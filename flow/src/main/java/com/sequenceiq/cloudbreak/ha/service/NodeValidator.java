package com.sequenceiq.cloudbreak.ha.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.domain.Node;

@Component
public class NodeValidator {

    @Value("${flow.trigger.node.heartbeat.validation.enabled:false}")
    private boolean nodeHeartbeatValidationEnabled;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private NodeService nodeService;

    @Inject
    private Clock clock;

    public void checkForRecentHeartbeat() {
        if (nodeHeartbeatValidationEnabled) {
            String selfId = nodeConfig.getId();
            if (StringUtils.isNotEmpty(selfId)) {
                Optional<Node> self = nodeService.findById(selfId);
                if (self.isPresent() && Math.abs(self.get().getLastUpdated() - clock.getCurrentTimeMillis()) >= TimeUnit.MINUTES.toMillis(1)) {
                    throw new CloudbreakServiceException("Current node's last heartbeat has happened more than a minute ago!");
                } else if (self.isEmpty()) {
                    throw new CloudbreakServiceException("Current node is not present in the database!");
                }
            }
        }
    }
}
