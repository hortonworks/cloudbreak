package com.sequenceiq.cloudbreak.ccmimpl.termination;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class DefaultCcmV2AgentTerminationListener implements CcmV2AgentTerminationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmV2AgentTerminationListener.class);

    @Inject
    private CcmV2ManagementClient ccmV2Client;

    @Override
    public void deregisterInvertingProxyAgent(String ccmV2AgentCrn) {
        if (ccmV2AgentCrn != null) {
            ccmV2Client.deregisterInvertingProxyAgent(MDCBuilder.getOrGenerateRequestId(), ccmV2AgentCrn);
        } else {
            LOGGER.info("Cluster ccmV2AgentCrn is not initialized, nothing to unregister.");
        }
    }
}
