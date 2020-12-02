package com.sequenceiq.cloudbreak.ccmimpl.termination;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CRN;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class DefaultCcmV2AgentTerminationListener implements CcmV2AgentTerminationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmV2AgentTerminationListener.class);

    @Inject
    private CcmV2ManagementClient ccmV2Client;

    @Override
    public void deregisterInvertingProxyAgent(Json ccmV2Configs) {
        if (ccmV2Configs != null) {
            String ccmV2AgentCrn = ((Map<String, String>) ccmV2Configs.getSilent(Map.class)).get(CCMV2_AGENT_CRN);
            String requestId = Optional.ofNullable(MDCBuilder.getMdcContextMap()
                    .get(LoggerContextKey.REQUEST_ID.toString())).orElse(UUID.randomUUID().toString());
            MDCBuilder.addRequestId(requestId);
            ccmV2Client.deregisterInvertingProxyAgent(requestId, ccmV2AgentCrn);
        } else {
            LOGGER.info("Cluster ccmV2AgentCrn is not initialized, nothing to unregister.");
        }
    }
}
