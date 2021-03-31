package com.sequenceiq.cloudbreak.telemetry.nodestatus;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NodeStatusConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusConfigService.class);

    public NodeStatusConfigView createNodeStatusConfig(String cdpNodeStatusUser, char[] cdpNodeStatusPassword) {
        NodeStatusConfigView.Builder builder = new NodeStatusConfigView.Builder();
        if (cdpNodeStatusPassword != null && StringUtils.isNoneBlank(cdpNodeStatusUser, new String(cdpNodeStatusPassword))) {
            LOGGER.debug("Filling authentication settings for nodestatus monitor.");
            builder.withServerUsername(cdpNodeStatusUser);
            builder.withServerPassword(cdpNodeStatusPassword);
        } else {
            LOGGER.debug("Authentication settings are missing from nodestatus monitor.");
        }
        return builder.build();
    }

}
