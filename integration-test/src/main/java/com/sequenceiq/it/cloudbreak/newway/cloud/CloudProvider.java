package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;

public abstract class CloudProvider {

    public static final String CREDENTIAL_DEFAULT_DESCRIPTION = "test credential";

    public abstract StackEntity aValidStackRequest();

    public abstract CredentialEntity aValidCredential();

    public abstract CredentialEntity aValidCredential(boolean create);

    public abstract AmbariV4Request ambariRequestWithBlueprintId(Long id);

    public abstract Stack aValidStackCreated();

    public abstract StackEntity aValidAttachedStackRequest();

    public abstract AmbariV4Request ambariRequestWithBlueprintName(String bluePrintName);

    public abstract AmbariV4Request ambariRequestWithBlueprintNameAndCustomAmbari(String bluePrintName, String customAmbariVersion,
            String customAmbariRepoUrl, String customAmbariRepoGpgKey);

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

    public abstract NetworkV4Request newNetwork();

    public abstract NetworkV4Request existingNetwork();

    public abstract NetworkV4Request existingSubnet();

    public abstract String availabilityZone();

    public abstract List<InstanceGroupV4Request> instanceGroups(String securityGroupId);

    public abstract List<InstanceGroupV4Request> instanceGroups(Set<String> recipes);

    public abstract Stack aValidAttachedClusterStackCreated(HostGroupType... groupTypes);

    public abstract AmbariV4Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName);

    public abstract ResourceHelper<?> getResourceHelper();

    public abstract Cluster aValidDatalakeCluster();

    public abstract Cluster aValidAttachedCluster(String datalakeClusterName);

    public abstract InstanceTemplateV4Request template();

    public abstract List<InstanceGroupV4Request> instanceGroups();

    public abstract List<InstanceGroupV4Request> instanceGroups(HostGroupType... groupTypes);

    public abstract List<InstanceGroupV4Request> instanceGroups(String securityGroupId, HostGroupType... groupTypes);

    public abstract List<InstanceGroupV4Request> instanceGroups(Set<String> recipes, HostGroupType... groupTypes);

    public abstract RecoveryMode getRecoveryModeParam(String hostgroupName);
}