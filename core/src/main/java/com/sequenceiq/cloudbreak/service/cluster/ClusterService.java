package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ZOMBIE;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPO_ID_TAG;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_HOST_STATUS_UPDATED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;
import static com.sequenceiq.common.api.type.CertExpirationState.HOST_CERT_EXPIRING;
import static com.sequenceiq.common.api.type.CertExpirationState.VALID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    private static final String RANGER_RAZ = "RANGER_RAZ";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterRepository repository;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private StackUpdater stackUpdater;

    public Cluster saveClusterAndComponent(Cluster cluster, List<ClusterComponent> components, String stackName) {
        Cluster savedCluster;
        try {
            long start = System.currentTimeMillis();
            savedCluster = measure(() -> repository.save(cluster),
                    LOGGER,
                    "Cluster repository save {} ms");
            if (savedCluster.getGateway() != null) {
                measure(() -> gatewayService.save(savedCluster.getGateway()),
                        LOGGER,
                        "gatewayService repository save {} ms");
            }
            LOGGER.debug("Cluster object saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            measure(() -> clusterComponentConfigProvider.store(components, savedCluster),
                    LOGGER,
                    "clusterComponentConfigProvider repository save {} ms");
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.CLUSTER, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedCluster;
    }

    public Optional<Cluster> findByNameAndWorkspace(String clusterName, Workspace workspace) {
        return repository.findByNameAndWorkspace(clusterName, workspace);
    }

    public Cluster saveWithRef(Cluster cluster) {
        Cluster savedCluster;
        try {
            long start = System.currentTimeMillis();
            if (cluster.getFileSystem() != null) {
                cluster.getFileSystem().setWorkspace(cluster.getWorkspace());
                fileSystemConfigService.pureSave(cluster.getFileSystem());
            }

            if (cluster.getAdditionalFileSystem() != null) {
                cluster.getAdditionalFileSystem().setWorkspace(cluster.getWorkspace());
                fileSystemConfigService.pureSave(cluster.getAdditionalFileSystem());
            }

            savedCluster = save(cluster);
            Gateway gateway = cluster.getGateway();
            if (gateway != null) {
                gateway.setCluster(savedCluster);
                gatewayService.save(gateway);
            }
            List<ClusterComponent> store = clusterComponentConfigProvider.store(cluster.getComponents(), savedCluster);
            savedCluster.setComponents(new HashSet<>(store));
            LOGGER.info("Cluster object saved in {} ms with cluster id {}", System.currentTimeMillis() - start, cluster.getId());
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.CLUSTER, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedCluster;
    }

    public Iterable<Cluster> saveAll(Iterable<Cluster> clusters) {
        return repository.saveAll(clusters);
    }

    public Cluster save(Cluster cluster) {
        return repository.save(cluster);
    }

    public Optional<Cluster> retrieveClusterByStackIdWithoutAuth(Long stackId) {
        return repository.findOneByStackId(stackId);
    }

    public Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig) {
        Cluster cluster = getCluster(clusterId);
        cluster.setClusterManagerIp(ambariClientConfig.getApiAddress());
        cluster = repository.save(cluster);
        LOGGER.info("Updated cluster: [ambariIp: '{}'].", ambariClientConfig.getApiAddress());
        return cluster;
    }

    public void updateInstancesToRunning(Long stackId, Set<Node> reachableNodes) {
        List<InstanceMetadataView> createdInstances = instanceMetaDataService.getAllStatusInForStack(stackId, Set.of(CREATED));
        List<Long> instanceMetadataIds = new ArrayList<>();
        for (InstanceMetadataView instanceMetaData : createdInstances) {
            if (reachableNodes.stream().anyMatch(node -> node.getHostname().equals(instanceMetaData.getDiscoveryFQDN()))) {
                instanceMetadataIds.add(instanceMetaData.getId());
            }
        }
        instanceMetaDataService.updateAllInstancesToStatus(instanceMetadataIds, SERVICES_RUNNING, "Services are running");
    }

    public void updateInstancesToZombie(Long stackId, Set<Node> unreachableNodes) {
        Set<String> unreachableInstanceIds = unreachableNodes.stream()
                .map(Node::getInstanceId)
                .collect(Collectors.toSet());
        LOGGER.debug("Update instance statuses to ZOMBIE, instanceIds: {}", unreachableInstanceIds);
        List<? extends InstanceMetadataView> notDeletedInstanceMetadatas = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stackId);
        List<Long> instanceMetadataIds = notDeletedInstanceMetadatas.stream()
                .filter(instanceMetadata -> unreachableInstanceIds.contains(instanceMetadata.getInstanceId()))
                .map(InstanceMetadataView::getId)
                .collect(Collectors.toList());
        instanceMetaDataService.updateAllInstancesToStatus(instanceMetadataIds, ZOMBIE, "Detected as Zombie instance metadata");
    }

    public String getStackRepositoryJson(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        StackRepoDetails repoDetails = clusterComponentConfigProvider.getStackRepoDetails(cluster.getId());
        String stackRepoId = repoDetails.getStack().get(REPO_ID_TAG);
        return clusterApiConnectors.getConnector(stackDto).clusterModificationService().getStackRepositoryJson(repoDetails, stackRepoId);
    }

    public void cleanupCluster(StackDtoDelegate stackDto) {
        if (stackDto.getCluster() == null || StringUtils.isEmpty(stackDto.getCluster().getClusterManagerIp())) {
            LOGGER.debug("Cluster server IP was not set before, cleanup cluster operation can be skipped.");
        } else {
            StackView stack = stackDto.getStack();
            try {
                Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
                clusterApiConnectors.getConnector(stackDto).clusterModificationService().cleanupCluster(telemetry);
                altusMachineUserService.clearFluentMachineUser(stack, stackDto.getCluster(), telemetry);
            } catch (CloudbreakServiceException se) {
                LOGGER.error("Cluster specific cleanup failed during obtaining telemetry component.", se);
            } catch (CloudbreakException e) {
                LOGGER.error("Cluster specific cleanup failed.", e);
            }
        }
    }

    public boolean withEmbeddedClusterManagerDB(ClusterView cluster) {
        DatabaseType databaseType = DatabaseType.CLOUDERA_MANAGER;
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutClusterService.findByClusterIdAndType(cluster.getId(), databaseType);
        return (rdsConfig == null || ResourceStatus.DEFAULT == rdsConfig.getStatus()) && cluster.getDatabaseServerCrn() == null;
    }

    public Cluster updateClusterStatusByStackId(Long stackId, DetailedStackStatus detailedStackStatus, String statusReason) {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, detailedStackStatus, statusReason);
        Stack stack = stackUpdater.updateStackStatus(stackId, detailedStackStatus, statusReason);
        return stack.getCluster();
    }

    public Cluster updateClusterStatusByStackId(Long stackId, DetailedStackStatus detailedStackStatus) {
        return updateClusterStatusByStackId(stackId, detailedStackStatus, "");
    }

    public Cluster updateClusterStatusByStackIdOutOfTransaction(Long stackId, DetailedStackStatus detailedStackStatus) throws TransactionExecutionException {
        return transactionService.notSupported(() -> updateClusterStatusByStackId(stackId, detailedStackStatus, ""));
    }

    public Cluster updateCluster(Cluster cluster) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster = repository.save(cluster);
        return cluster;
    }

    public void updateCreationDateOnCluster(Long clusterId) {
        repository.updateCreationStartedByClusterId(clusterId, new Date().getTime());
    }

    public void updateClusterMetadata(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        if (!connector.clusterStatusService().isClusterManagerRunning()) {
            Set<InstanceMetaData> notTerminatedInstanceMetaDatas = instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stackId);
            InstanceMetaData cmInstance = updateClusterManagerHostStatus(notTerminatedInstanceMetaDatas);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), CLUSTER_HOST_STATUS_UPDATED,
                    Arrays.asList(cmInstance.getDiscoveryFQDN(), cmInstance.getInstanceStatus().name()));
        } else {
            ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses(
                    runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()));
            updateClusterCertExpirationState(stack.getCluster(), extendedHostStatuses.isAnyCertExpiring());
            List<InstanceMetadataView> notTerminatedInstanceMetaDatas = stack.getNotTerminatedInstanceMetaData();
            List<InstanceMetadataView> updatedInstanceMetaData = updateInstanceStatuses(notTerminatedInstanceMetaDatas, extendedHostStatuses);
            fireHostStatusUpdateNotification(stackId, updatedInstanceMetaData);
        }
    }

    private InstanceMetaData updateClusterManagerHostStatus(Set<InstanceMetaData> notTerminatedInstanceMetaDatas) {
        InstanceMetaData cmInstance = notTerminatedInstanceMetaDatas.stream().filter(InstanceMetaData::getClusterManagerServer).findFirst()
                .orElseThrow(() -> new CloudbreakServiceException("Cluster manager inaccessible, and the corresponding instance metadata not found."));
        cmInstance.setInstanceStatus(SERVICES_UNHEALTHY);
        cmInstance.setStatusReason("Cluster manager inaccessible.");
        instanceMetaDataService.save(cmInstance);
        return cmInstance;
    }

    private void fireHostStatusUpdateNotification(Long stackId, List<InstanceMetadataView> updatedInstanceMetaData) {
        updatedInstanceMetaData.forEach(instanceMetaData -> {
            eventService.fireCloudbreakEvent(stackId, AVAILABLE.name(), CLUSTER_HOST_STATUS_UPDATED,
                    Arrays.asList(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getInstanceStatus().name()));
        });
    }

    private List<InstanceMetadataView> updateInstanceStatuses(List<InstanceMetadataView> notTerminatedInstanceMetaDatas,
            ExtendedHostStatuses extendedHostStatuses) {
        return notTerminatedInstanceMetaDatas.stream()
                .filter(instanceMetaData -> SERVICES_RUNNING.equals(instanceMetaData.getInstanceStatus()))
                .map(instanceMetaData -> {
                    HostName hostName = hostName(instanceMetaData.getDiscoveryFQDN());
                    if (extendedHostStatuses.getHostsHealth().containsKey(hostName)) {
                        InstanceStatus newState = extendedHostStatuses.isHostHealthy(hostName) ? SERVICES_HEALTHY : SERVICES_UNHEALTHY;
                        String reason = extendedHostStatuses.statusReasonForHost(hostName);
                        instanceMetaDataService.updateInstanceStatus(instanceMetaData, newState, reason);
                    }
                    return instanceMetaData;
                }).collect(Collectors.toList());
    }

    public void updateClusterCertExpirationState(Long stackId, boolean hostCertificateExpiring) {
        Optional<Cluster> cluster = findOneByStackId(stackId);
        cluster.ifPresent(c -> updateClusterCertExpirationState(c, hostCertificateExpiring));
    }

    public void updateClusterCertExpirationState(ClusterView cluster, boolean hostCertificateExpiring) {
        if (VALID == cluster.getCertExpirationState() && hostCertificateExpiring) {
            LOGGER.info("Update cert expiration state from {} to {}", cluster.getCertExpirationState(), HOST_CERT_EXPIRING);
            repository.updateCertExpirationState(cluster.getId(), HOST_CERT_EXPIRING);
        } else if (HOST_CERT_EXPIRING == cluster.getCertExpirationState() && !hostCertificateExpiring) {
            LOGGER.info("Update cert expiration state from {} to {}", cluster.getCertExpirationState(), VALID);
            repository.updateCertExpirationState(cluster.getId(), VALID);
        }
    }

    public boolean isRangerRazEnabledOnCluster(StackDto stack) {
        ClusterView cluster = stack.getCluster();
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stack);
        if (!clusterApi.clusterStatusService().isClusterManagerRunning()) {
            throw new BadRequestException(String.format("Cloudera Manager is not running for cluster: %s", cluster.getName()));
        }
        return clusterApi.clusterModificationService().isServicePresent(cluster.getName(), RANGER_RAZ);
    }

    public Cluster prepareCluster(Collection<HostGroup> hostGroups, Blueprint blueprint, long stackId, Cluster cluster) {
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().clear();
        cluster.getHostGroups().addAll(hostGroups);
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        cluster.setStack(stackDtoService.getStackReferenceById(stackId));
        cluster = repository.save(cluster);
        return cluster;
    }

    public Cluster getClusterByStackResourceCrn(String stackResourceCrn) {
        return repository.findByStackResourceCrn(stackResourceCrn).orElseThrow(notFound("Cluster by stack resource CRN",
                stackResourceCrn));
    }

    public Cluster getCluster(Long clusterId) {
        return repository.findById(clusterId)
                .orElseThrow(notFound("Cluster", clusterId));
    }

    public Cluster getByIdWithLists(Long id) {
        return repository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("Cluster '%s' not found", id)));
    }

    public Map<HostName, String> getHostStatuses(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        return clusterApiConnectors.getConnector(stackDto).clusterStatusService().getHostStatusesRaw();
    }

    public Set<Cluster> findByBlueprint(Blueprint blueprint) {
        return repository.findByBlueprint(blueprint);
    }

    public Set<Cluster> findByCustomConfigurations(CustomConfigurations customConfigurations) {
        return repository.findByCustomConfigurations(customConfigurations);
    }

    public List<Cluster> findByStatuses(Collection<Status> statuses) {
        return repository.findByStatuses(statuses);
    }

    public Optional<Cluster> findOneByStackId(Long stackId) {
        return repository.findOneByStackId(stackId);
    }

    public Cluster findOneByStackIdOrNotFoundError(Long stackId) {
        return repository.findOneByStackId(stackId).orElseThrow(notFound("Cluster by stack ID", stackId));
    }

    public Optional<Cluster> findOneWithLists(Long id) {
        return repository.findOneWithLists(id);
    }

    public Cluster findOneWithCustomConfigurations(Long id) {
        return repository.findOneWithCustomConfigurations(id).orElseThrow(notFound("Cluster", id));
    }

    public Optional<Cluster> findById(Long clusterId) {
        return repository.findById(clusterId);
    }

    public Set<Cluster> findByRdsConfig(Long rdsConfigId) {
        return repository.findByRdsConfig(rdsConfigId);
    }

    public Set<String> findNamesByRdsConfig(Long rdsConfigId) {
        return repository.findNamesByRdsConfig(rdsConfigId);
    }

    public void pureDelete(Cluster cluster) {
        repository.delete(cluster);
    }

    public void updateCreationFinishedAndUpSinceToNowByClusterId(Long clusterId) {
        long now = System.currentTimeMillis();
        repository.updateCreationFinishedAndUpSinceByClusterId(clusterId, now);
    }

    public void updatedUpSinceToNowByClusterId(Long clusterId) {
        long now = System.currentTimeMillis();
        repository.updateUpSinceByClusterId(clusterId, now);
    }

    public void updateExtendedBlueprintText(Long clusterId, String template) {
        Cluster cluster = getCluster(clusterId);
        cluster.setExtendedBlueprintText(template);
        repository.save(cluster);
    }

    public void updateDatabusCredentialByClusterId(Long clusterId, String databusCredentialJsonString) {
        repository.updateDatabusCredentialByClusterId(clusterId, databusCredentialJsonString);
    }

    public void addRdsConfigToCluster(Long newRdsConfigId, Long clusterId) {
        repository.addRdsConfigToCluster(clusterId, newRdsConfigId);
    }

    public void updateFqdnOnCluster(Long clusterId, String fqdn) {
        repository.updateFqdnByClusterId(clusterId, fqdn);
    }

    public void updateClusterManagerIp(Long clusterId, String gatewayIp) {
        repository.updateClusterManagerIp(clusterId, gatewayIp);
    }

    public Cluster getClusterReference(Long id) {
        return repository.getById(id);
    }
}
