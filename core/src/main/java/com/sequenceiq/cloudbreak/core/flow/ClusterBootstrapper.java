package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class ClusterBootstrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);

    @Value("${cb.container.orchestrator:SWARM}")
    private ContainerOrchestratorTool containerOrchestratorTool;

    @javax.annotation.Resource
    private Map<ContainerOrchestratorTool, ContainerOrchestrator> containerOrchestrators;

    @Autowired
    private StackRepository stackRepository;

    public FlowContext bootstrapCluster(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
            Set<String> dataVolumes = new HashSet<>();
            for (int i = 1; i <= volumeCount; i++) {
                dataVolumes.add("/hadoopfs/fs" + i);
            }
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), getHostname(instanceMetaData.getLongName()), dataVolumes));
        }

        ContainerOrchestrator containerOrchestrator = containerOrchestrators.get(containerOrchestratorTool);
        ContainerOrchestratorCluster cluster = containerOrchestrator.bootstrap(stack, gatewayInstance.getPublicIp(), nodes, stack.getConsulServers());
        containerOrchestrator.startRegistrator(cluster);
        containerOrchestrator.startAmbariServer(cluster);
        containerOrchestrator.startAmbariAgents(cluster, cluster.getNodes().size() - 1);
        containerOrchestrator.startConsulWatches(cluster, cluster.getNodes().size());

        return new ProvisioningContext.Builder()
                .setAmbariIp(provisioningContext.getAmbariIp())
                .setDefaultParams(stack.getId(), stack.cloudPlatform())
                .build();
    }

    public void bootstrapNewNodes(ClusterScalingContext clusterScalingContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(clusterScalingContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (clusterScalingContext.getUpscaleCandidateAddresses().contains(instanceMetaData.getPrivateIp())) {
                int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
                Set<String> dataVolumes = new HashSet<>();
                for (int i = 1; i <= volumeCount; i++) {
                    dataVolumes.add("/hadoopfs/fs" + i);
                }
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), getHostname(instanceMetaData.getLongName()), dataVolumes));
            }
        }

        ContainerOrchestrator containerOrchestrator = containerOrchestrators.get(containerOrchestratorTool);
        ContainerOrchestratorCluster cluster = containerOrchestrator.bootstrapNewNodes(stack, gatewayInstance.getPublicIp(), nodes);
        containerOrchestrator.startAmbariAgents(cluster, cluster.getNodes().size());
        containerOrchestrator.startConsulWatches(cluster, cluster.getNodes().size());
    }

    private String getHostname(String longName) {
        return longName.split("\\.")[0];
    }

}
