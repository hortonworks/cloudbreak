package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_BAYWATCH_ENABLED;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_BAYWATCH_EXTERN_LOCATION;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_DB;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_WARMUP;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_BAYWATCH_CLIENT;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_BAYWATCH_SERVER;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_REGISTRATOR;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.StackDeletionBasedExitCriteriaModel.stackDeletionBasedExitCriteriaModel;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

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

    @Value("${cb.docker.container.baywatch.server:" + CB_DOCKER_CONTAINER_BAYWATCH_SERVER + "}")
    private String baywatchServerDockerImageName;

    @Value("${cb.docker.container.baywatch.client:" + CB_DOCKER_CONTAINER_BAYWATCH_CLIENT + "}")
    private String baywatchClientDockerImageName;

    @Value("${cb.baywatch.extern.location:" + CB_BAYWATCH_EXTERN_LOCATION + "}")
    private String baywatchServerExternLocation;

    @Value("${cb.baywatch.enabled:" + CB_BAYWATCH_ENABLED + "}")
    private Boolean baywatchEnabled;

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
        GatewayConfig gatewayConfig = new GatewayConfig(gatewayInstance.getPublicIp(), stack.getCertDir());

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
            ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
            containerOrchestrator.startRegistrator(cluster, registratorDockerImageName, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startAmbariServer(cluster, postgresDockerImageName, getAmbariImageName(stack), cloudPlatform,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startAmbariAgents(cluster, getAmbariImageName(stack), cluster.getNodes().size() - 1, cloudPlatform,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startConsulWatches(cluster, consulWatchPlugnDockerImageName, cluster.getNodes().size(),
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            if (baywatchEnabled) {
                if (StringUtils.isEmpty(baywatchServerExternLocation)) {
                    containerOrchestrator.startBaywatchServer(cluster, baywatchServerDockerImageName, stackDeletionBasedExitCriteriaModel(stack.getId()));
                }
                containerOrchestrator.startBaywatchClients(cluster, baywatchClientDockerImageName, gatewayInstance.getPrivateIp(), cluster.getNodes().size(),
                        ConsulUtils.CONSUL_DOMAIN, baywatchServerExternLocation, stackDeletionBasedExitCriteriaModel(stack.getId()));
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new FlowCancelledException(e.getMessage());
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
        GatewayConfig gatewayConfig = new GatewayConfig(gatewayInstance.getPublicIp(), stack.getCertDir());

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
            ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
            containerOrchestrator.startAmbariAgents(cluster, getAmbariImageName(stack), cluster.getNodes().size(), cloudPlatform,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startConsulWatches(cluster, consulWatchPlugnDockerImageName, cluster.getNodes().size(),
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            if (baywatchEnabled) {
                containerOrchestrator.startBaywatchClients(cluster, baywatchClientDockerImageName, gatewayInstance.getPrivateIp(),
                        cluster.getNodes().size(), ConsulUtils.CONSUL_DOMAIN, baywatchServerExternLocation, stackDeletionBasedExitCriteriaModel(stack.getId()));
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new FlowCancelledException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private String getAmbariImageName(Stack stack) {
        return stack.getCluster().getAmbariStackDetails() == null ? warmAmbariDockerImageName : ambariDockerImageName;
    }
}
