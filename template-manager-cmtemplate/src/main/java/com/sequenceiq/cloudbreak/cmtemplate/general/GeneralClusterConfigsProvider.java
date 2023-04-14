package com.sequenceiq.cloudbreak.cmtemplate.general;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class GeneralClusterConfigsProvider {

    public static final String KAFKA_BROKER = "KAFKA_BROKER";

    private static final String PENDING_DEFAULT_VALUE = "pending...";

    public GeneralClusterConfigs generalClusterConfigs(StackDtoDelegate stack, Credential credential) {
        ClusterView cluster = stack.getCluster();
        boolean gatewayInstanceMetadataPresented = false;
        boolean instanceMetadataPresented = false;
        if (stack.getInstanceGroupDtos() != null && !stack.getInstanceGroupDtos().isEmpty()) {
            List<InstanceMetadataView> gatewayInstanceMetadata = stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata();
            gatewayInstanceMetadataPresented = !gatewayInstanceMetadata.isEmpty() && stack.hasGateway();
            instanceMetadataPresented = true;
        }
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterManagerIp(cluster.getClusterManagerIp());
        generalClusterConfigs.setInstanceGroupsPresented(instanceMetadataPresented);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(gatewayInstanceMetadataPresented);
        generalClusterConfigs.setClusterName(cluster.getName());
        generalClusterConfigs.setStackName(stack.getName());
        generalClusterConfigs.setUuid(stack.getUuid());
        generalClusterConfigs.setUserName(cluster.getUserName());
        generalClusterConfigs.setPassword(cluster.getPassword());
        generalClusterConfigs.setCloudbreakClusterManagerUser(cluster.getCloudbreakClusterManagerUser());
        generalClusterConfigs.setCloudbreakClusterManagerPassword(cluster.getCloudbreakClusterManagerPassword());
        generalClusterConfigs.setNodeCount(stack.getFullNodeCount().intValue());
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.ofNullable(stack.getPrimaryGatewayInstance().getDiscoveryFQDN()));
        generalClusterConfigs.setOtherGatewayInstancesDiscoveryFQDN(
                getOtherGatewayInstancesFqdns(stack.getPrimaryGatewayInstance(), stack.getAllAvailableGatewayInstances()));
        generalClusterConfigs.setVariant(cluster.getVariant());
        generalClusterConfigs.setAutoTlsEnabled(cluster.getAutoTlsEnabled());
        boolean userFacingCertHasBeenGenerated = StringUtils.isNotEmpty(stack.getSecurityConfig().getUserFacingKey())
                && StringUtils.isNotEmpty(stack.getSecurityConfig().getUserFacingCert());
        generalClusterConfigs.setKnoxUserFacingCertConfigured(userFacingCertHasBeenGenerated);
        generalClusterConfigs.setExternalFQDN(cluster.getFqdn());
        generalClusterConfigs.setEnableRangerRaz(cluster.isRangerRazEnabled());
        generalClusterConfigs.setGovCloud(credential.isGovCloud());
        generalClusterConfigs.setCreatorWorkloadUserCrn(stack.getCreator().getUserCrn());
        return generalClusterConfigs;
    }

    public GeneralClusterConfigs generalClusterConfigs(StackV4Request stack, Credential credential, String email, String clusterVariant) {
        boolean gatewayInstanceMetadataPresented = false;
        int nodeCount = 0;
        for (InstanceGroupV4Request instanceGroup : stack.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setIdentityUserEmail(email);

        generalClusterConfigs.setClusterManagerIp(PENDING_DEFAULT_VALUE);
        generalClusterConfigs.setInstanceGroupsPresented(false);
        generalClusterConfigs.setPassword(stack.getCluster().getPassword());
        if (stack.getCluster().getGateway() != null) {
            gatewayInstanceMetadataPresented = true;
        }
        generalClusterConfigs.setGatewayInstanceMetadataPresented(gatewayInstanceMetadataPresented);
        generalClusterConfigs.setClusterName(stack.getName());
        generalClusterConfigs.setStackName(stack.getName());
        generalClusterConfigs.setUuid(PENDING_DEFAULT_VALUE);
        generalClusterConfigs.setUserName(stack.getCluster().getUserName());
        generalClusterConfigs.setNodeCount(nodeCount);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(PENDING_DEFAULT_VALUE));
        generalClusterConfigs.setVariant(clusterVariant);
        Boolean autoTlsEnabled = Optional.ofNullable(stack.getCluster().getCm())
                .map(ClouderaManagerV4Request::getEnableAutoTls)
                .orElse(Boolean.FALSE);
        generalClusterConfigs.setAutoTlsEnabled(autoTlsEnabled);
        generalClusterConfigs.setGovCloud(credential.isGovCloud());
        return generalClusterConfigs;
    }

    private Set<String> getOtherGatewayInstancesFqdns(InstanceMetadataView primaryGateway, List<InstanceMetadataView> allInstancesMetadata) {
        List<InstanceMetadataView> otherInstanceMetadata = new ArrayList<>(allInstancesMetadata);
        otherInstanceMetadata.remove(primaryGateway);
        return otherInstanceMetadata.stream().map(im -> im.getDiscoveryFQDN()).collect(Collectors.toSet());
    }
}
