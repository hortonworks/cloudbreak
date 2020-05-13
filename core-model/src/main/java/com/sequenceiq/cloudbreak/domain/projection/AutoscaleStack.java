package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;

public interface AutoscaleStack {

    Long getId();

    String getName();

    String getOwner();

    Integer getGatewayPort();

    Long getCreated();

    Status getStackStatus();

    Secret getCloudbreakAmbariUser();

    Secret getCloudbreakAmbariPassword();

    Status getClusterStatus();

    InstanceGroupType getInstanceGroupType();

    InstanceMetadataType getInstanceMetadataType();

    String getPublicIp();

    String getPrivateIp();

    Boolean getUsePrivateIpToTls();

    String getTenantName();

    Long getWorkspaceId();

    String getUserId();

    String getUserCrn();

    String getClusterManagerVariant();

    String getCrn();

    String getCloudPlatform();

    StackType getType();

    Tunnel getTunnel();
}