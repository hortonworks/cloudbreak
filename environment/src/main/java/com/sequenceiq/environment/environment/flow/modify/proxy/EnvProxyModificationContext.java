package com.sequenceiq.environment.environment.flow.modify.proxy;

import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class EnvProxyModificationContext extends CommonContext {

    private final ProxyConfig proxyConfig;

    private final ProxyConfig previousProxyConfig;

    public EnvProxyModificationContext(FlowParameters flowParameters, ProxyConfig proxyConfig, ProxyConfig previousProxyConfig) {
        super(flowParameters);
        this.proxyConfig = proxyConfig;
        this.previousProxyConfig = previousProxyConfig;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public ProxyConfig getPreviousProxyConfig() {
        return previousProxyConfig;
    }
}
