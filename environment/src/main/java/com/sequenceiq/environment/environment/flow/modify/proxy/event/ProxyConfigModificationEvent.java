package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import com.sequenceiq.environment.environment.flow.EnvironmentEvent;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public interface ProxyConfigModificationEvent extends EnvironmentEvent {

    ProxyConfig getProxyConfig();
}
