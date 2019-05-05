package com.sequenceiq.cloudbreak.cloud.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.Payload;

public interface InstancePayload extends Payload {
    Set<String> getInstanceIds();
}
