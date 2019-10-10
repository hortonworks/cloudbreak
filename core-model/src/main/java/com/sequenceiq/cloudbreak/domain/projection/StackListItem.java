package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.json.Json;

public interface StackListItem {

    Long getId();

    String getName();

    String getBlueprintName();

    Json getBlueprintTags();

    Status getStackStatus();

    Status getClusterStatus();

    Long getCreated();

    String getStackType();

    String getStackVersion();

    String getCloudPlatform();

    Boolean getGovCloud();

    Long getSharedClusterId();

    Long getClusterId();

    String getCredentialName();

    String getPlatformVariant();

    Boolean getSecure();

    String getAmbariIp();

    Long getBlueprintId();

    Integer getHostGroupCount();

    ResourceStatus getBlueprintStatus();

    Long getTerminated();
}
