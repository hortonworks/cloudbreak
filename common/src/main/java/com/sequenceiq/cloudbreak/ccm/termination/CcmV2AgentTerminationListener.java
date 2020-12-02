package com.sequenceiq.cloudbreak.ccm.termination;

public interface CcmV2AgentTerminationListener {

    void deregisterInvertingProxyAgent(String ccmV2AgentCrn);
}
