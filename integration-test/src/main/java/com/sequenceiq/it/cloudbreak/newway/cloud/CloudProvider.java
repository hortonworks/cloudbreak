package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;

import java.util.List;
import java.util.Map;

public abstract class CloudProvider {
    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "test credential";

    public abstract StackEntity aValidStackRequest();

    public abstract CredentialEntity aValidCredential();

    public abstract AmbariV2Request ambariRequestWithBlueprintId(Long id);

    public abstract Stack aValidStackIsCreated();

    public abstract AmbariV2Request ambariRequestWithBlueprintName(String blueprintHdp26EdwanalyticsName);

    public abstract String getClusterName();

    public abstract String getPlatform();

    public abstract String getCredentialName();

    public abstract String getBlueprintName();

    public abstract String getNetworkName();

    public abstract String getSubnetCIDR();

    public abstract Map<String, Object> newNetworkProperties();

    public abstract Map<String, Object> networkProperties();

    public abstract Map<String, Object> subnetProperties();

    public abstract String getVpcId();

    public abstract String getSubnetId();

    public abstract String region();

    public abstract NetworkV2Request newNetwork();

    public abstract NetworkV2Request existingNetwork();

    public abstract NetworkV2Request existingSubnet();

    public abstract String availabilityZone();

    public abstract void setClusterNamePostfix(String clusterNamePostfix);

    public abstract String getClusterNamePostfix();

    public abstract InstanceGroupV2Request hostgroup(String groupName, InstanceGroupType groupType, int nodeCount);

    public abstract List<InstanceGroupV2Request> instanceGroups(String securityGroupId);
}
