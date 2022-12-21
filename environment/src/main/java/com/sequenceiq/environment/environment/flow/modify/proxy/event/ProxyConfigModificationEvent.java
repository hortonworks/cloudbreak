package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public interface ProxyConfigModificationEvent extends ResourceCrnPayload {

    String getResourceName();

    Promise<AcceptResult> accepted();

    String getProxyConfigCrn();

    String getPreviousProxyConfigCrn();
}
