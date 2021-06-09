package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPO_ID_TAG;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_HOST_STATUS_UPDATED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;
import static com.sequenceiq.common.api.type.CertExpirationState.HOST_CERT_EXPIRING;
import static com.sequenceiq.common.api.type.CertExpirationState.VALID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState.ClusterManagerStatus;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterRepository repository;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private UsageLoggingUtil usageLoggingUtil;

    public Cluster saveClusterAndComponent(Cluster cluster, List<ClusterComponent> components, String stackName) {
        Cluster savedCluster;
        try {
            long start = System.currentTimeMillis();
            savedCluster =  measure(() -> repository.save(cluster),
                    LOGGER,
                    "Cluster repository save {} ms");
            if (savedCluster.getGateway() != null) {
                measure(() ->  gatewayService.save(savedCluster.getGateway()),
                        LOGGER,
                        "gatewayService repository save {} ms");
            }
            LOGGER.debug("Cluster object saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            measure(() ->  clusterComponentConfigProvider.store(components, savedCluster),
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

    public boolean isSingleNode(Stack stack) {
        int nodeCount = 0;
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            nodeCount += ig.getNodeCount();
        }
        return nodeCount == 1;
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

    public void updateInstancesToRunning(Long clusterId, Map<String, List<String>> hostsPerHostGroup) {
        try {
            transactionService.required(() -> {
                for (Entry<String, List<String>> hostGroupEntry : hostsPerHostGroup.entrySet()) {
                    hostGroupService.getByClusterIdAndName(clusterId, hostGroupEntry.getKey()).ifPresent(hostGroup -> {
                        hostGroup.getInstanceGroup().getUnattachedInstanceMetaDataSet()
                                .forEach(instanceMetaData -> {
                                    instanceMetaData.setInstanceStatus(SERVICES_RUNNING);
                                    instanceMetaDataService.save(instanceMetaData);
                                });
                    });
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public String getStackRepositoryJson(Long stackId) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        StackRepoDetails repoDetails = clusterComponentConfigProvider.getStackRepoDetails(cluster.getId());
        String stackRepoId = repoDetails.getStack().get(REPO_ID_TAG);
        return clusterApiConnectors.getConnector(stack).clusterModificationService().getStackRepositoryJson(repoDetails, stackRepoId);
    }

    public void cleanupCluster(final Long stackId) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                if (StringUtils.isEmpty(stack.getCluster().getClusterManagerIp())) {
                    LOGGER.debug("Cluster server IP was not set before, cleanup cluster operation can be skipped.");
                } else {
                    Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
                    try {
                        clusterApiConnectors.getConnector(stack).clusterModificationService().cleanupCluster(telemetry);
                        altusMachineUserService.clearFluentMachineUser(stack, telemetry);
                    } catch (CloudbreakException e) {
                        LOGGER.error("Cluster specific cleanup failed.", e);
                    }
                }
                return stack;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public boolean withEmbeddedClusterManagerDB(Cluster cluster) {
        DatabaseType databaseType = DatabaseType.CLOUDERA_MANAGER;
        RDSConfig rdsConfig = rdsConfigService.findByClusterIdAndType(cluster.getId(), databaseType);
        return (rdsConfig == null || ResourceStatus.DEFAULT == rdsConfig.getStatus()) && cluster.getDatabaseServerCrn() == null;
    }

    public Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason) {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, status, statusReason);
        StackStatus stackStatus = stackService.getCurrentStatusByStackId(stackId);
        Optional<Cluster> cluster = retrieveClusterByStackIdWithoutAuth(stackId);
        Optional<Status> clusterOldStatus = cluster.map(Cluster::getStatus);
        cluster = cluster.map(c -> {
            c.setStatus(status);
            c.setStatusReason(statusReason);
            return c;
        }).map(repository::save);
        handleInMemoryState(stackId, stackStatus, cluster, clusterOldStatus);
        return cluster.orElse(null);
    }

    private void handleInMemoryState(Long stackId, StackStatus stackStatus, Optional<Cluster> cluster, Optional<Status> clusterOldStatus) {
        cluster.ifPresent(c -> {
            clusterOldStatus.ifPresent(oldstatus -> usageLoggingUtil.logClusterStatusChangeUsageEvent(oldstatus, c));
            if (c.getStatus().isRemovableStatus()) {
                InMemoryStateStore.deleteCluster(c.getId());
                if (stackStatus.getStatus().isRemovableStatus()) {
                    InMemoryStateStore.deleteStack(stackId);
                }
            } else {
                InMemoryStateStore.putCluster(c.getId(), statusToPollGroupConverter.convert(c.getStatus()));
                if (InMemoryStateStore.getStack(stackId) == null) {
                    InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(stackStatus.getStatus()));
                }
            }
        });
    }

    public Cluster updateClusterStatusByStackId(Long stackId, Status status) {
        return updateClusterStatusByStackId(stackId, status, "");
    }

    public Cluster updateClusterStatusByStackIdOutOfTransaction(Long stackId, Status status) throws TransactionExecutionException {
        return transactionService.notSupported(() -> updateClusterStatusByStackId(stackId, status, ""));
    }

    public Cluster updateCluster(Cluster cluster) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster = repository.save(cluster);
        return cluster;
    }

    public Cluster updateCreationDateOnCluster(Cluster cluster) {
        if (cluster.getCreationStarted() == null) {
            cluster.setCreationStarted(new Date().getTime());
            cluster = updateCluster(cluster);
        }
        return cluster;
    }

    public Cluster updateClusterMetadata(Long stackId) {
        Stack stack = stackService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        if (!connector.clusterStatusService().isClusterManagerRunning()) {
            Set<InstanceMetaData> notTerminatedInstanceMetaDatas = instanceMetaDataService.findNotTerminatedForStack(stackId);
            InstanceMetaData cmInstance = updateClusterManagerHostStatus(notTerminatedInstanceMetaDatas);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), CLUSTER_HOST_STATUS_UPDATED,
                    Arrays.asList(cmInstance.getDiscoveryFQDN(), cmInstance.getInstanceStatus().name()));
            return stack.getCluster();
        } else {
            ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses();
            updateClusterCertExpirationState(stack.getCluster(), extendedHostStatuses.isHostCertExpiring());
            Map<HostName, ClusterManagerState> hostStatuses = extendedHostStatuses.getHostHealth();
            try {
                return transactionService.required(() -> {
                    Set<InstanceMetaData> notTerminatedInstanceMetaDatas = instanceMetaDataService.findNotTerminatedForStack(stackId);
                    List<InstanceMetaData> updatedInstanceMetaData = updateInstanceStatuses(notTerminatedInstanceMetaDatas, hostStatuses);
                    instanceMetaDataService.saveAll(updatedInstanceMetaData);
                    fireHostStatusUpdateNotification(stack, updatedInstanceMetaData);
                    return stack.getCluster();
                });
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
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

    private void fireHostStatusUpdateNotification(Stack stack, List<InstanceMetaData> updatedInstanceMetaData) {
        updatedInstanceMetaData.forEach(instanceMetaData -> {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), CLUSTER_HOST_STATUS_UPDATED,
                    Arrays.asList(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getInstanceStatus().name()));
        });
    }

    private List<InstanceMetaData> updateInstanceStatuses(Set<InstanceMetaData> notTerminatedInstanceMetaDatas, Map<HostName,
            ClusterManagerState> hostStatuses) {
        return notTerminatedInstanceMetaDatas.stream()
                .filter(instanceMetaData -> SERVICES_RUNNING.equals(instanceMetaData.getInstanceStatus()))
                .map(instanceMetaData -> {
                    ClusterManagerState clusterManagerState = hostStatuses.get(hostName(instanceMetaData.getDiscoveryFQDN()));
                    if (clusterManagerState != null) {
                        InstanceStatus newState = ClusterManagerStatus.HEALTHY.equals(clusterManagerState.getClusterManagerStatus()) ?
                                SERVICES_HEALTHY : SERVICES_UNHEALTHY;
                        instanceMetaData.setInstanceStatus(newState);
                        instanceMetaData.setStatusReason(clusterManagerState.getStatusReason());
                    }
                    return instanceMetaData;
                }).collect(Collectors.toList());
    }

    public void updateClusterCertExpirationState(Long stackId, boolean hostCertificateExpiring) {
        Optional<Cluster> cluster = findOneByStackId(stackId);
        cluster.ifPresent(c -> updateClusterCertExpirationState(c, hostCertificateExpiring));
    }

    public void updateClusterCertExpirationState(Cluster cluster, boolean hostCertificateExpiring) {
        if (VALID == cluster.getCertExpirationState() && hostCertificateExpiring) {
            LOGGER.info("Update cert expiration state from {} to {}", cluster.getCertExpirationState(), HOST_CERT_EXPIRING);
            repository.updateCertExpirationState(cluster.getId(), HOST_CERT_EXPIRING);
        } else if (HOST_CERT_EXPIRING == cluster.getCertExpirationState() && !hostCertificateExpiring) {
            LOGGER.info("Update cert expiration state from {} to {}", cluster.getCertExpirationState(), VALID);
            repository.updateCertExpirationState(cluster.getId(), VALID);
        }
    }

    public Cluster prepareCluster(Collection<HostGroup> hostGroups, Blueprint blueprint, Stack stack, Cluster cluster) {
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().clear();
        cluster.getHostGroups().addAll(hostGroups);
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        cluster.setStatus(REQUESTED);
        cluster.setStack(stack);
        cluster = repository.save(cluster);
        return cluster;
    }

    public Cluster getCluster(Stack stack) {
        return getCluster(stack.getCluster().getId());
    }

    private Cluster getCluster(Long clusterId) {
        return repository.findById(clusterId)
                .orElseThrow(notFound("Cluster", clusterId));
    }

    public Cluster getById(Long id) {
        return repository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("Cluster '%s' not found", id)));
    }

    public Map<HostName, String> getHostStatuses(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        return clusterApiConnectors.getConnector(stack).clusterStatusService().getHostStatusesRaw();
    }

    public Set<Cluster> findByBlueprint(Blueprint blueprint) {
        return repository.findByBlueprint(blueprint);
    }

    public List<Cluster> findByStatuses(Collection<Status> statuses) {
        return repository.findByStatuses(statuses);
    }

    public Optional<Cluster> findOneByStackId(Long stackId) {
        return repository.findOneByStackId(stackId);
    }

    public Optional<Cluster> findOneWithLists(Long id) {
        return repository.findOneWithLists(id);
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

}
