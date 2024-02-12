package com.sequenceiq.cloudbreak.domain.view;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

public interface BaseBlueprintClusterView {
    String getName();

    StackType getType();

    Long getId();
}
