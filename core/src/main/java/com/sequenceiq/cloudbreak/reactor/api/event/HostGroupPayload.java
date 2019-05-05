package com.sequenceiq.cloudbreak.reactor.api.event;

import com.sequenceiq.cloudbreak.common.event.Payload;

public interface HostGroupPayload extends Payload {
    String getHostGroupName();
}
