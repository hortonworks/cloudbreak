package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.api.type.Tunnel;

public interface StackListItem {
    Long getId();

    String getResourceCrn();

    String getName();

    String getEnvironmentCrn();

    StackType getType();

    String getBlueprintCrn();

    String getBlueprintName();

    Json getBlueprintTags();

    Long getBlueprintCreated();

    Status getStackStatus();

    Status getClusterStatus();

    Long getCreated();

    String getStackType();

    String getStackVersion();

    String getCloudPlatform();

    String getDatalakeCrn();

    Long getClusterId();

    String getPlatformVariant();

    String getClusterManagerIp();

    Long getBlueprintId();

    Integer getHostGroupCount();

    ResourceStatus getBlueprintStatus();

    BlueprintUpgradeOption getBlueprintUpgradeOption();

    Long getTerminated();

    Long getUserDOId();

    String getUserId();

    String getUsername();

    String getUsercrn();

    Tunnel getTunnel();

    CertExpirationState getCertExpirationState();
}
