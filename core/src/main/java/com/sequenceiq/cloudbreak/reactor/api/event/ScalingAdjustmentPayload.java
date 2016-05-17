package com.sequenceiq.cloudbreak.reactor.api.event;

public interface ScalingAdjustmentPayload extends HostGroupPayload {
    Integer getScalingAdjustment();
}
