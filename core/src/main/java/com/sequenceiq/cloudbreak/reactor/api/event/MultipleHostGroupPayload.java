package com.sequenceiq.cloudbreak.reactor.api.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;

public interface MultipleHostGroupPayload extends Payload {
    Set<String> getHostGroupNames();
}
