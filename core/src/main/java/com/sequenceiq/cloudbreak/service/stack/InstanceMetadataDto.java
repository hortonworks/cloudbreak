package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;

public interface InstanceMetadataDto {

   Long getId();

   InstanceStatus getInstanceStatus();

   String getInstanceName();

   String getStatusReason();

   Long getPrivateId();

   String getPrivateIp();

   String getPublicIp();

   Integer getSshPort();

   String getInstanceId();

   Boolean getAmbariServer();

   Boolean getClusterManagerServer();

   String getDiscoveryFQDN();

   InstanceMetadataType getInstanceMetadataType();

   String getLocalityIndicator();

   Long getStartDate();

   Long getTerminationDate();

   String getSubnetId();

   String getAvailabilityZone();

   String getRackId();

   InstanceLifeCycle getLifeCycle();

   String getVariant();

   Json getImage();

   Long getInstanceGroupId();
}
