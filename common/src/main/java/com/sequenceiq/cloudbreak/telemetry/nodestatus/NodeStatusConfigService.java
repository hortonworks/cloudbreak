package com.sequenceiq.cloudbreak.telemetry.nodestatus;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.context.NodeStatusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

@Service
public class NodeStatusConfigService implements TelemetryPillarConfigGenerator<NodeStatusConfigView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusConfigService.class);

    private static final String SALT_STATE = "nodestatus";

    public NodeStatusConfigView createNodeStatusConfig(String cdpNodeStatusUser, char[] cdpNodeStatusPassword, boolean saltPingEnabled) {
        NodeStatusConfigView.Builder builder = new NodeStatusConfigView.Builder();
        if (cdpNodeStatusPassword != null && StringUtils.isNoneBlank(cdpNodeStatusUser, new String(cdpNodeStatusPassword))) {
            LOGGER.debug("Filling authentication settings for nodestatus monitor.");
            builder.withServerUsername(cdpNodeStatusUser);
            builder.withServerPassword(cdpNodeStatusPassword);
        } else {
            LOGGER.debug("Authentication settings are missing from nodestatus monitor.");
        }
        builder.withSaltPingEnabled(saltPingEnabled);
        return builder.build();
    }

    @Override
    public NodeStatusConfigView createConfigs(TelemetryContext context) {
        NodeStatusContext nodeStatusContext = context.getNodeStatusContext();
        return new NodeStatusConfigView.Builder()
                .withServerUsername(nodeStatusContext.getUsername())
                .withServerPassword(nodeStatusContext.getPassword())
                .withSaltPingEnabled(nodeStatusContext.isSaltPingEnabled())
                .build();
    }

    @Override
    public boolean isEnabled(TelemetryContext context) {
        return context != null && context.getNodeStatusContext() != null;
    }

    @Override
    public String saltStateName() {
        return SALT_STATE;
    }
}
