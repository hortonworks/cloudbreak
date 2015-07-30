package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_BAYWATCH_ENABLED;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_BAYWATCH_EXTERN_LOCATION;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_DB;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_AMBARI_WARMUP;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_BAYWATCH_CLIENT;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_BAYWATCH_SERVER;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_KERBEROS;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_LOGROTATE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_DOCKER_CONTAINER_REGISTRATOR;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.StackDeletionBasedExitCriteriaModel.stackDeletionBasedExitCriteriaModel;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
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
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Component
public class ClusterContainerRunner {

    private static final String CONTAINER_VOLUME_PATH = "/var/log";
    private static final String BAYWATCH_CONTAINER_VOLUME_PATH = CONTAINER_VOLUME_PATH + "/containers";
    private static final String HOST_VOLUME_PATH = VolumeUtils.getLogVolume("logs");

    @Value("${cb.docker.container.ambari.warm:}")
    private String warmAmbariDockerImageName;

    @Value("${cb.docker.container.ambari:}")
    private String ambariDockerImageName;

    @Value("${cb.docker.container.registrator:" + CB_DOCKER_CONTAINER_REGISTRATOR + "}")
    private String registratorDockerImageName;

    @Value("${cb.docker.container.docker.consul.watch.plugn:" + CB_DOCKER_CONTAINER_DOCKER_CONSUL_WATCH_PLUGN + "}")
    private String consulWatchPlugnDockerImageName;

    @Value("${cb.docker.container.ambari.db:" + CB_DOCKER_CONTAINER_AMBARI_DB + "}")
    private String postgresDockerImageName;

    @Value("${cb.docker.container.kerberos:" + CB_DOCKER_CONTAINER_KERBEROS + "}")
    private String kerberosDockerImageName;

    @Value("${cb.docker.container.baywatch.server:" + CB_DOCKER_CONTAINER_BAYWATCH_SERVER + "}")
    private String baywatchServerDockerImageName;

    @Value("${cb.docker.container.baywatch.client:" + CB_DOCKER_CONTAINER_BAYWATCH_CLIENT + "}")
    private String baywatchClientDockerImageName;

    @Value("${cb.docker.container.logrotate:" + CB_DOCKER_CONTAINER_LOGROTATE + "}")
    private String logrotateDockerImageName;

    @Value("${cb.baywatch.extern.location:" + CB_BAYWATCH_EXTERN_LOCATION + "}")
    private String baywatchServerExternLocation;

    @Value("${cb.baywatch.enabled:" + CB_BAYWATCH_ENABLED + "}")
    private Boolean baywatchEnabled;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void runClusterContainers(ProvisioningContext provisioningContext) throws CloudbreakException {
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
        String cloudPlatform = provisioningContext.getCloudPlatform().name();

        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIp(), gatewayInstance.getPrivateIp());

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
            Set<String> dataVolumes = new HashSet<>();
            for (int i = 1; i <= volumeCount; i++) {
                dataVolumes.add(VolumeUtils.VOLUME_PREFIX + i);
            }
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryName(), dataVolumes));
        }
        try {
            Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
            LogVolumePath logVolumePath = new LogVolumePath(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH);

            ContainerOrchestratorCluster orchestratorCluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
            containerOrchestrator.startRegistrator(orchestratorCluster, registratorDockerImageName, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startAmbariServer(orchestratorCluster, postgresDockerImageName, getAmbariImageName(stack), cloudPlatform,
                    logVolumePath, cluster.isSecure(), stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startAmbariAgents(orchestratorCluster, getAmbariImageName(stack), orchestratorCluster.getNodes().size() - 1, cloudPlatform,
                    logVolumePath, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startConsulWatches(orchestratorCluster, consulWatchPlugnDockerImageName, orchestratorCluster.getNodes().size(),
                    logVolumePath, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startLogrotate(orchestratorCluster, logrotateDockerImageName, orchestratorCluster.getNodes().size(),
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            if (cluster.isSecure()) {
                KerberosConfiguration kerberosConfiguration = new KerberosConfiguration(cluster.getKerberosMasterKey(), cluster.getKerberosAdmin(),
                        cluster.getKerberosPassword());
                containerOrchestrator.startKerberosServer(orchestratorCluster, kerberosDockerImageName, logVolumePath, kerberosConfiguration,
                        stackDeletionBasedExitCriteriaModel(stack.getId()));
            }
            if (baywatchEnabled) {
                if (StringUtils.isEmpty(baywatchServerExternLocation)) {
                    containerOrchestrator.startBaywatchServer(orchestratorCluster, baywatchServerDockerImageName,
                            stackDeletionBasedExitCriteriaModel(stack.getId()));
                }
                LogVolumePath baywatchLogVolumePath = new LogVolumePath(HOST_VOLUME_PATH, BAYWATCH_CONTAINER_VOLUME_PATH);
                containerOrchestrator.startBaywatchClients(orchestratorCluster, baywatchClientDockerImageName, orchestratorCluster.getNodes().size(),
                        ConsulUtils.CONSUL_DOMAIN, baywatchLogVolumePath, baywatchServerExternLocation, stackDeletionBasedExitCriteriaModel(stack.getId()));
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
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIp(), gatewayInstance.getPrivateIp());

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (context.getUpscaleCandidateAddresses().contains(instanceMetaData.getPrivateIp())) {
                int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
                Set<String> dataVolumes = new HashSet<>();
                for (int i = 1; i <= volumeCount; i++) {
                    dataVolumes.add(VolumeUtils.VOLUME_PREFIX + i);
                }
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryName(), dataVolumes));
            }
        }
        try {
            LogVolumePath logVolumePath = new LogVolumePath(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH);

            ContainerOrchestratorCluster cluster = new ContainerOrchestratorCluster(gatewayConfig, nodes);
            containerOrchestrator.startAmbariAgents(cluster, getAmbariImageName(stack), cluster.getNodes().size(), cloudPlatform,
                    logVolumePath, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startConsulWatches(cluster, consulWatchPlugnDockerImageName, cluster.getNodes().size(),
                    logVolumePath, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containerOrchestrator.startLogrotate(cluster, logrotateDockerImageName, cluster.getNodes().size(),
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
            if (baywatchEnabled) {
                LogVolumePath baywatchLogVolumePath = new LogVolumePath(HOST_VOLUME_PATH, BAYWATCH_CONTAINER_VOLUME_PATH);
                containerOrchestrator.startBaywatchClients(cluster, baywatchClientDockerImageName, cluster.getNodes().size(),
                        ConsulUtils.CONSUL_DOMAIN, baywatchLogVolumePath, baywatchServerExternLocation, stackDeletionBasedExitCriteriaModel(stack.getId()));
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new FlowCancelledException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private String getAmbariImageName(Stack stack) {
        String imageName;
        if (stack.getCluster().getAmbariStackDetails() == null) {
            imageName = determineImageName(warmAmbariDockerImageName, CB_DOCKER_CONTAINER_AMBARI_WARMUP);
        } else {
            imageName = determineImageName(ambariDockerImageName, CB_DOCKER_CONTAINER_AMBARI);
        }
        return imageName;
    }

    private String determineImageName(String imageName, String defaultImageName) {
        if (Strings.isNullOrEmpty(imageName)) {
            return defaultImageName;
        }
        return imageName;
    }
}
