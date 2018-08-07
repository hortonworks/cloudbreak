package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;

public abstract class CloudProvider {

    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "test credential";

    public abstract StackEntity aValidStackRequest();

    public abstract CredentialEntity aValidCredential();

    public abstract CredentialEntity aValidCredential(boolean create);

    public abstract AmbariV2Request ambariRequestWithBlueprintId(Long id);

    public abstract Stack aValidStackIsCreated();

    public abstract StackEntity aValidAttachedStackRequest();

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

    public abstract List<InstanceGroupV2Request> instanceGroups(String securityGroupId);

    public abstract List<InstanceGroupV2Request> instanceGroups(Set<String> recipes);

    public abstract Stack aValidAttachedClusterStackIsCreated(HostGroupType... groupTypes);

    public abstract AmbariV2Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName);

    public abstract ResourceHelper<?> getResourceHelper();

    public abstract Cluster aValidDatalakeCluster();

    public abstract Cluster aValidAttachedCluster(String datalakeClusterName);

    public abstract TemplateV2Request template();

    public abstract List<InstanceGroupV2Request> instanceGroups();

    public abstract List<InstanceGroupV2Request> instanceGroups(HostGroupType... groupTypes);

    public abstract List<InstanceGroupV2Request> instanceGroups(String securityGroupId, HostGroupType... groupTypes);

    public abstract List<InstanceGroupV2Request> instanceGroups(Set<String> recipes, HostGroupType... groupTypes);
}