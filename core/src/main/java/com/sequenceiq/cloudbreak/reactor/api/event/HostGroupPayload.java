package com.sequenceiq.cloudbreak.reactor.api.event;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

public interface HostGroupPayload extends Payload {
    String getHostGroupName();
}
