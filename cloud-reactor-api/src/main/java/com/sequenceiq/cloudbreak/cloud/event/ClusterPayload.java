package com.sequenceiq.cloudbreak.cloud.event;

public interface ClusterPayload extends Payload {
    Long getClusterId();
}
