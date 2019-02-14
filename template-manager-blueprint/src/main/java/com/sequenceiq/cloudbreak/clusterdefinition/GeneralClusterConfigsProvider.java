package com.sequenceiq.cloudbreak.clusterdefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Service
public class GeneralClusterConfigsProvider {

    public static final String KAFKA_BROKER = "KAFKA_BROKER";

    private static final int DEFAULT_REPLICATION_FACTOR = 3;

    private static final String PENDING_DEFAULT_VALUE = "pending...";

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    public GeneralClusterConfigs generalClusterConfigs(Stack stack, Cluster cluster) {
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
        String clusterDefinitionText = cluster.getClusterDefinition().getClusterDefinitionText();
        generalClusterConfigs.setKafkaReplicationFactor(
                getKafkaReplicationFactor(clusterDefinitionText) >= DEFAULT_REPLICATION_FACTOR ? DEFAULT_REPLICATION_FACTOR : 1);

        return generalClusterConfigs;
    }

    public GeneralClusterConfigs generalClusterConfigs(StackV4Request stack, User user, String email) {
        boolean gatewayInstanceMetadataPresented = false;
        int nodeCount = 0;
        for (InstanceGroupV4Request instanceGroup : stack.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }

        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setIdentityUserEmail(email);

        generalClusterConfigs.setAmbariIp(PENDING_DEFAULT_VALUE);
        generalClusterConfigs.setInstanceGroupsPresented(false);
        generalClusterConfigs.setPassword(stack.getCluster().getAmbari().getPassword());
        if (stack.getCluster().getGateway() != null) {
            gatewayInstanceMetadataPresented = true;
        }
        generalClusterConfigs.setGatewayInstanceMetadataPresented(gatewayInstanceMetadataPresented);
        generalClusterConfigs.setClusterName(stack.getName());
        generalClusterConfigs.setExecutorType(stack.getCluster().getExecutorType());
        generalClusterConfigs.setStackName(stack.getName());
        generalClusterConfigs.setUuid(PENDING_DEFAULT_VALUE);
        generalClusterConfigs.setUserName(stack.getCluster().getAmbari().getUserName());
        generalClusterConfigs.setNodeCount(nodeCount);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of(PENDING_DEFAULT_VALUE));
        generalClusterConfigs.setKafkaReplicationFactor(1);

        return generalClusterConfigs;
    }

    private int getKafkaReplicationFactor(String blueprintText) {
        int kafkaBrokerNumber = 0;
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
        Map<String, Set<String>> componentsByHostGroup = ambariBlueprintTextProcessor.getComponentsByHostGroup();
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
