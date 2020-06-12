package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

public interface ClusterTemplateStatusView {

    ResourceStatus getStatus();
}
