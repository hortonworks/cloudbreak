package com.sequenceiq.environment.environment.flow.modify.proxy;

import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class EnvProxyModificationContext extends CommonContext {

    private final ProxyConfig previousProxyConfig;

    public EnvProxyModificationContext(FlowParameters flowParameters, ProxyConfig previousProxyConfig) {
        super(flowParameters);
        this.previousProxyConfig = previousProxyConfig;
    }

    public ProxyConfig getPreviousProxyConfig() {
        return previousProxyConfig;
    }
}
