package com.sequenceiq.cloudbreak.blueprint;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Service
public class GeneralClusterConfigsProvider {

    public static final String KAFKA_BROKER = "KAFKA_BROKER";

    public static final int DEFAULT_REPLICATION_FACTOR = 3;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    public GeneralClusterConfigs generalClusterConfigs(Stack stack, Cluster cluster, CloudbreakUser cloudbreakUser) {
        boolean gatewayInstanceMetadataPresented = false;
        boolean instanceMetadataPresented = false;
        if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
            List<InstanceMetaData> gatewayInstanceMetadata = stack.getGatewayInstanceMetadata();
            gatewayInstanceMetadataPresented = !gatewayInstanceMetadata.isEmpty()
                    && stack.getCluster().getGateway() != null
                    && stack.getCluster().getGateway().isGatewayEnabled();
            instanceMetadataPresented = true;
        }
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setIdentityUserEmail(cloudbreakUser.getUsername());
        generalClusterConfigs.setAmbariIp(cluster.getAmbariIp());
        generalClusterConfigs.setInstanceGroupsPresented(instanceMetadataPresented);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(gatewayInstanceMetadataPresented);
        generalClusterConfigs.setClusterName(cluster.getName());
        generalClusterConfigs.setExecutorType(cluster.getExecutorType());
        generalClusterConfigs.setStackName(stack.getName());
        generalClusterConfigs.setUuid(stack.getUuid());
        generalClusterConfigs.setUserName(cluster.getUserName());
        generalClusterConfigs.setPassword(cluster.getPassword());
        generalClusterConfigs.setNodeCount(stack.getFullNodeCount());
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.ofNullable(stack.getPrimaryGatewayInstance().getDiscoveryFQDN()));
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        generalClusterConfigs.setKafkaReplicationFactor(
                getKafkaReplicationFactor(blueprintText) >= DEFAULT_REPLICATION_FACTOR ? DEFAULT_REPLICATION_FACTOR : 1);

        return generalClusterConfigs;
    }

    public GeneralClusterConfigs generalClusterConfigs(StackV2Request stack, User user, String email) {
        boolean gatewayInstanceMetadataPresented = false;
        boolean instanceMetadataPresented = false;
        int nodeCount = 0;
        for (InstanceGroupV2Request instanceGroupV2Request : stack.getInstanceGroups()) {
            nodeCount += instanceGroupV2Request.getNodeCount();
        }

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setIdentityUserEmail(email);

        generalClusterConfigs.setAmbariIp("pending...");
        generalClusterConfigs.setInstanceGroupsPresented(instanceMetadataPresented);
        generalClusterConfigs.setPassword(stack.getCluster().getAmbari().getPassword());
        if (stack.getCluster().getAmbari().getGateway() != null) {
            gatewayInstanceMetadataPresented = true;
        }
        generalClusterConfigs.setGatewayInstanceMetadataPresented(gatewayInstanceMetadataPresented);
        generalClusterConfigs.setClusterName(stack.getGeneral().getName());
        generalClusterConfigs.setExecutorType(stack.getCluster().getExecutorType());
        generalClusterConfigs.setStackName(stack.getGeneral().getName());
        generalClusterConfigs.setUuid("pending...");
        generalClusterConfigs.setUserName(stack.getCluster().getAmbari().getUserName());
        generalClusterConfigs.setNodeCount(nodeCount);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.ofNullable("pending..."));
        generalClusterConfigs.setKafkaReplicationFactor(1);

        return generalClusterConfigs;
    }

    private int getKafkaReplicationFactor(String blueprintText) {
        int kafkaBrokerNumber = 0;
        BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(blueprintText);
        Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
        for (Map.Entry<String, Set<String>> hostGroup : componentsByHostGroup.entrySet()) {
            for (String service : hostGroup.getValue()) {
                if (KAFKA_BROKER.equals(service)) {
                    kafkaBrokerNumber++;
                }
            }
        }
        return kafkaBrokerNumber;
    }
}
