package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.StackDeletionBasedExitCriteriaModel.stackDeletionBasedExitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.CONSUL_WATCH;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.HAVEGED;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.KERBEROS;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.LOGROTATE;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.REGISTRATOR;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;

@Component
public class ClusterContainerRunner {

    private static final String CONTAINER_VOLUME_PATH = "/var/log";
    private static final String HOST_VOLUME_PATH = VolumeUtils.getLogVolume("logs");

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ContainerConfigService containerConfigService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void runClusterContainers(ProvisioningContext context) throws CloudbreakException {
        try {
            initializeClusterContainers(context, false, Collections.<String>emptySet());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public void addClusterContainers(ClusterScalingContext context) throws CloudbreakException {
        try {
            initializeClusterContainers(context, true, context.getUpscaleCandidateAddresses());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private void initializeClusterContainers(DefaultFlowContext context, Boolean add, Set<String> candidateAddresses) throws CloudbreakException,
            CloudbreakOrchestratorException {
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
        String cloudPlatform = context.getCloudPlatform().value();

        Stack stack = stackRepository.findOneWithLists(context.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIp(), gatewayInstance.getPrivateIp());

        Set<Node> nodes = getNodes(add, stack, candidateAddresses);

        LogVolumePath logVolumePath = new LogVolumePath(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH);
        ContainerOrchestratorCluster orchestratorCluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
        if (!add) {
            Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
            containerOrchestrator.startRegistrator(orchestratorCluster, containerConfigService.get(stack, REGISTRATOR),
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startAmbariServer(orchestratorCluster, containerConfigService.get(stack, AMBARI_DB),
                    containerConfigService.get(stack, AMBARI_SERVER), cloudPlatform, logVolumePath, stackDeletionBasedExitCriteriaModel(stack.getId()));
            if (cluster.isSecure()) {
                containerOrchestrator.startHaveged(orchestratorCluster, containerConfigService.get(stack, HAVEGED),
                        stackDeletionBasedExitCriteriaModel(stack.getId()));
                KerberosConfiguration kerberosConfiguration = new KerberosConfiguration(cluster.getKerberosMasterKey(), cluster.getKerberosAdmin(),
                        cluster.getKerberosPassword());
                containerOrchestrator.startKerberosServer(orchestratorCluster, containerConfigService.get(stack, KERBEROS), logVolumePath,
                        kerberosConfiguration, stackDeletionBasedExitCriteriaModel(stack.getId()));
            }
        }
        containerOrchestrator.startAmbariAgents(orchestratorCluster, containerConfigService.get(stack, AMBARI_AGENT), cloudPlatform, logVolumePath,
                stackDeletionBasedExitCriteriaModel(stack.getId()));
        containerOrchestrator.startConsulWatches(orchestratorCluster, containerConfigService.get(stack, CONSUL_WATCH), logVolumePath,
                stackDeletionBasedExitCriteriaModel(stack.getId()));
        containerOrchestrator.startLogrotate(orchestratorCluster, containerConfigService.get(stack, LOGROTATE),
                stackDeletionBasedExitCriteriaModel(stack.getId()));
    }

    private Set<Node> getNodes(Boolean add, Stack stack, Set<String> candidateAddresses) {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (!add || candidateAddresses.contains(instanceMetaData.getPrivateIp())) {
                int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
                Set<String> dataVolumes = new HashSet<>();
                for (int i = 1; i <= volumeCount; i++) {
                    dataVolumes.add(VolumeUtils.VOLUME_PREFIX + i);
                }
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryName(), dataVolumes));
            }
        }

        return nodes;
    }


}
