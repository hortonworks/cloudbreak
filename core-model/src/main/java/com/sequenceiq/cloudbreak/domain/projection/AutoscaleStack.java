package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.domain.Secret;

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
}