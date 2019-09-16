package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.model.Status;

public interface StackListItem {

    Long getId();

    String getName();

    String getBlueprintName();

    Status getStackStatus();

    Status getClusterStatus();

    Long getCreated();

    String getStackType();

    String getStackVersion();

    String getCloudPlatform();
}
