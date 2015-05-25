package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_DB;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_WARMUP;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_REGISTRATOR;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class ClusterContainerRunner {

    @Value("${cb.docker.container.ambari.warm:" + CB_DOCKER_CONTAINER_AMBARI_WARMUP + "}")
    private String warmAmbariDockerImageName;

    @Value("${cb.docker.container.ambari:" + CB_DOCKER_CONTAINER_AMBARI + "}")
    private String ambariDockerImageName;

    @Value("${cb.docker.container.registrator:" + CB_DOCKER_CONTAINER_REGISTRATOR + "}")
    private String registratorDockerImageName;

    @Value("${cb.docker.container.docker.consul.watch.plugn:" + CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN + "}")
    private String consulWatchPlugnDockerImageName;

    @Value("${cb.docker.container.ambari.db:" + CB_DOCKER_CONTAINER_AMBARI_DB + "}")
    private String postgresDockerImageName;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    public void runClusterContainers(ProvisioningContext provisioningContext) throws CloudbreakException {
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
        String cloudPlatform = provisioningContext.getCloudPlatform().name();

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
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryName(), dataVolumes));
        }
        try {
            ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayInstance.getPublicIp(), nodes);
            containerOrchestrator.startRegistrator(cluster, registratorDockerImageName);
            containerOrchestrator.startAmbariServer(cluster, postgresDockerImageName, getAmbariImageName(stack), cloudPlatform);
            containerOrchestrator.startAmbariAgents(cluster, getAmbariImageName(stack), cluster.getNodes().size() - 1, cloudPlatform);
            containerOrchestrator.startConsulWatches(cluster, consulWatchPlugnDockerImageName, cluster.getNodes().size());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public void addClusterContainers(ClusterScalingContext context) throws CloudbreakException {
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
        String cloudPlatform = context.getCloudPlatform().name();

        Stack stack = stackRepository.findOneWithLists(context.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (context.getUpscaleCandidateAddresses().contains(instanceMetaData.getPrivateIp())) {
                int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
                Set<String> dataVolumes = new HashSet<>();
                for (int i = 1; i <= volumeCount; i++) {
                    dataVolumes.add("/hadoopfs/fs" + i);
                }
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryName(), dataVolumes));
            }
        }
        try {
            ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayInstance.getPublicIp(), nodes);
            containerOrchestrator.startAmbariAgents(cluster, getAmbariImageName(stack), cluster.getNodes().size(), cloudPlatform);
            containerOrchestrator.startConsulWatches(cluster, consulWatchPlugnDockerImageName, cluster.getNodes().size());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private String getAmbariImageName(Stack stack) {
        return stack.getCluster().getAmbariStackDetails() == null ? warmAmbariDockerImageName : ambariDockerImageName;
    }
}
