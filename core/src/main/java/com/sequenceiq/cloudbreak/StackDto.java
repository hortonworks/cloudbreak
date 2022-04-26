package com.sequenceiq.cloudbreak;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.common.api.type.Tunnel;

public interface StackDto {

    Long getId();

    String getResourceCrn();

    String getName();

    String getRegion();

    Integer getGatewayPort();

    Tunnel getTunnel();

    String getEnvironmentCrn();

    StackType getType();

    String getStackVersion();

    Status getStackStatus();

    String getStatusReason();

    String getCloudPlatform();

    Long getCreated();

    String getDatalakeCrn();

    Json getTags();

//    Long getClusterId();

    String getPlatformVariant();

    String getCustomDomain();

    String getCustomHostname();

    boolean isHostgroupNameAsHostname();

    boolean isClusterNameAsSubdomain();

    String getDisplayName();

    String getDescription();

    String getLoginUserName();

    String getPublicKey();

    String getPublicKeyId();

    Long getTerminated();

    Long getCreatorId();

    String getCreatorUserId();

    String getCreatorUsername();

    String getCreatorUserCrn();

    DatabaseAvailabilityType getExternalDatabaseCreationType();

    String getExternalDatabaseEngineVersion();

    Long getWorkspaceId();

    SecurityConfig getSecurityConfig();

}
