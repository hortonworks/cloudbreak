package com.sequenceiq.cloudbreak.cloud.event;

public interface InstancePayload extends Payload {
    String getInstanceId();
}
