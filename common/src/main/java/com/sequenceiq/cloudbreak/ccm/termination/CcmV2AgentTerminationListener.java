package com.sequenceiq.cloudbreak.ccm.termination;

import com.sequenceiq.cloudbreak.common.json.Json;

public interface CcmV2AgentTerminationListener {

    void deregisterInvertingProxyAgent(Json ccmV2Configs);
}
