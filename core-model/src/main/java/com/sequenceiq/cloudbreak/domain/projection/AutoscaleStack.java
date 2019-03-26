package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;

public interface AutoscaleStack {

    Long getId();

    String getName();

    String getOwner();

    Integer getGatewayPort();

    Long getCreated();

    Status getStackStatus();

    String getCloudbreakAmbariUser();

    String getCloudbreakAmbariPassword();

    Status getClusterStatus();

    InstanceGroupType getInstanceGroupType();

    InstanceMetadataType getInstanceMetadataType();

    String getPublicIp();

    String getPrivateIp();

    Boolean getUsePrivateIpToTls();
}
