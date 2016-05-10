package com.sequenceiq.cloudbreak.reactor.api.event;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

public interface ClusterUpscalePayload extends Payload {
    String getCloudPlatformName();
    String getHostGroupName();
    Integer getScalingAdjustment();
}
