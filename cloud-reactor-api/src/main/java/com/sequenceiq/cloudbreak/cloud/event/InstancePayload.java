package com.sequenceiq.cloudbreak.cloud.event;

import java.util.Set;

public interface InstancePayload extends Payload {
    Set<String> getInstanceIds();
}
