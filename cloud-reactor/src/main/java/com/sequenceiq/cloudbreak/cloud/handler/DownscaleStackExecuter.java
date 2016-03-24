package com.sequenceiq.cloudbreak.cloud.handler;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;

interface DownscaleStackExecuter {
    DownscaleStackResult execute(DownscaleStackRequest request);
}
