package com.sequenceiq.cloudbreak.domain.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_START_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.OnFailureActionConverter;
import com.sequenceiq.cloudbreak.common.domain.IdAware;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestrationNode;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.ArchitectureConverter;
import com.sequenceiq.cloudbreak.converter.ProviderSyncSetToStringConverter;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.converter.DnsResolverTypeConverter;
import com.sequenceiq.cloudbreak.domain.converter.StackTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ProviderSyncState;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name", "resourceCrn"}))
public class Stack implements ProvisionEntity, WorkspaceAwareResource, OrchestratorAware, StackView, StackDtoDelegate, IdAware {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String region;

    /**
     * Name of the availability zone the stack is deployed in. May be {@code null} if the cloud platform does not support this construct.
     *
     * @deprecated Use {@code InstanceMetaData.availabilityZone} instead (reachable via {@code instanceGroups.instanceMetaData}).
     */
    @Deprecated(since = "2.45.0")
    private String availabilityZone;

    private Integer gatewayPort;

    /**
     * @deprecated use {@link #tunnel} instead
     */
    @Deprecated
    private Boolean useCcm = Boolean.FALSE;

    @Convert(converter = TunnelConverter.class)
    private Tunnel tunnel = Tunnel.DIRECT;

    private int consulServers;

    private String customDomain;

    private String customHostname;

    private boolean hostgroupNameAsHostname;

    private boolean clusterNameAsSubdomain;

    private String displayName;

    private String resourceCrn;

    private String stackVersion;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "\"key\"")
    @Column(name = "\"value\"", columnDefinition = "TEXT", length = 100000)
    private Map<String, String> parameters;

    @Column(columnDefinition = "TEXT")
    private String platformVariant;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Cluster cluster;

    @OneToOne(cascade = CascadeType.ALL)
    private StackStatus stackStatus;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Resource> resources = new HashSet<>();

    @Column(nullable = false)
    @Convert(converter = OnFailureActionConverter.class)
    private OnFailureAction onFailureActionAction = OnFailureAction.DO_NOTHING;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private FailurePolicy failurePolicy;

    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private SecurityConfig securityConfig;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

    @Transient
    private Set<Component> components = new HashSet<>();

    @Version
    private Long version;

    @ManyToOne
    private Network network;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private StackAuthentication stackAuthentication;

    @OneToOne
    private Orchestrator orchestrator;

    private Long created;

    private Long terminated;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json inputs;

    private String uuid;

    @ManyToOne
    private Workspace workspace;

    @ManyToOne
    @JoinColumn(name = "createdBy")
    private User creator;

    private String environmentCrn;

    private String datalakeCrn;

    @Convert(converter = StackTypeConverter.class)
    private StackType type;

    private boolean clusterProxyRegistered;

    private String minaSshdServiceId;

    private String ccmV2AgentCrn;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "stack_id", referencedColumnName = "id")
    private Set<LoadBalancer> loadBalancers = new HashSet<>();

    @Convert(converter = DnsResolverTypeConverter.class)
    private DnsResolverType domainDnsResolver;

    private Integer javaVersion;

    /**
     * Specifically used for cases in which the stack has been detached during a resize operation.
     * This allows us to retain the original stack name which can be necessary when resources are defined around
     * the original name.
     */
    private String originalName;

    @NotNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "database_id")
    private Database database;

    private boolean multiAz;

    @Column(name = "creator_client")
    private String creatorClient;

    private String supportedImdsVersion;

    @Convert(converter = ArchitectureConverter.class)
    private Architecture architecture;

    @Convert(converter = ProviderSyncSetToStringConverter.class)
    private Set<ProviderSyncState> providerSyncStates = new HashSet<>();

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Stack instanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups.clear();
        Optional.ofNullable(instanceGroups).ifPresent(this.instanceGroups::addAll);
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public StackView getStack() {
        return this;
    }

    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public Blueprint getBlueprint() {
        return cluster.getBlueprint();
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public StackStatus getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(StackStatus stackStatus) {
        this.stackStatus = stackStatus;
    }

    public Status getStatus() {
        return stackStatus != null ? stackStatus.getStatus() : null;
    }

    @Override
    public DetailedStackStatus getDetailedStatus() {
        return stackStatus == null ? null : stackStatus.getDetailedStackStatus();
    }

    public String getStatusReason() {
        return stackStatus != null ? stackStatus.getStatusReason() : null;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public Stack resources(Set<Resource> resources) {
        this.resources.clear();
        Optional.ofNullable(resources).ifPresent(this.resources::addAll);
        return this;
    }

    public void setCreatorClient(String creatorClient) {
        this.creatorClient = creatorClient;
    }

    public int getConsulServers() {
        return consulServers;
    }

    public void setConsulServers(int consulServers) {
        this.consulServers = consulServers;
    }

    public OnFailureAction getOnFailureActionAction() {
        return onFailureActionAction;
    }

    public void setOnFailureActionAction(OnFailureAction onFailureActionAction) {
        this.onFailureActionAction = onFailureActionAction;
    }

    public FailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicy failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public boolean isUseCcm() {
        return Boolean.TRUE.equals(useCcm);
    }

    public void setUseCcm(Boolean useCcm) {
        this.useCcm = useCcm;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    /**
     * @deprecated please use PlatformAwareSdxConnector instead to find out related DL CRN by environmentCrn
     * or ensure to use this only in case of VM form DL deployment
     */
    @Deprecated
    public String getDatalakeCrn() {
        return datalakeCrn;
    }

    public void setDatalakeCrn(String datalakeCrn) {
        this.datalakeCrn = datalakeCrn;
    }

    public List<Resource> getDiskResources() {
        switch (platformVariant) {
            case CloudConstants.AWS:
            case CloudConstants.AWS_NATIVE:
            case CloudConstants.AWS_NATIVE_GOV:
                return getResourcesByType(ResourceType.AWS_VOLUMESET);
            case CloudConstants.GCP:
                return getResourcesByType(ResourceType.GCP_ATTACHED_DISKSET);
            case CloudConstants.AZURE:
                return ResourceUtil.getLatestResourceByInstanceId(getResourcesByType(ResourceType.AZURE_VOLUMESET));
            default:
                return List.of();
        }
    }

    public ResourceType getDiskResourceType() {
        switch (platformVariant) {
            case CloudConstants.AWS:
            case CloudConstants.AWS_NATIVE:
            case CloudConstants.AWS_NATIVE_GOV:
                return ResourceType.AWS_VOLUMESET;
            case CloudConstants.GCP:
                return ResourceType.GCP_ATTACHED_DISKSET;
            case CloudConstants.AZURE:
                return ResourceType.AZURE_VOLUMESET;
            case CloudConstants.MOCK:
                return ResourceType.MOCK_VOLUME;
            default:
                return null;
        }
    }

    public List<Resource> getResourcesByType(ResourceType resourceType) {
        List<Resource> resourceList = new ArrayList<>();
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourceList.add(resource);
            }
        }
        return resourceList;
    }

    public Resource getResourceByType(ResourceType resourceType) {
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                return resource;
            }
        }
        return null;
    }

    public InstanceGroup getInstanceGroupByInstanceGroupId(Long groupId) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (groupId.equals(instanceGroup.getId())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public InstanceGroupDto getInstanceGroupByInstanceGroupName(String group) {
        for (InstanceGroupDto instanceGroup : getInstanceGroupDtos()) {
            if (group.equals(instanceGroup.getInstanceGroup().getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public Long getFullNodeCount() {
        Integer nodeCount = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            nodeCount += instanceGroup.getNodeCount();
        }
        return nodeCount.longValue();
    }

    public Set<InstanceMetaData> getNotTerminatedAndNotZombieInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotTerminatedAndNotZombieInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotTerminatedInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotTerminatedInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public List<InstanceMetaData> getNotTerminatedAndNotZombieInstanceMetaDataList() {
        return new ArrayList<>(getNotTerminatedAndNotZombieInstanceMetaDataSet());
    }

    public Set<InstanceMetaData> getNotDeletedAndNotZombieInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getZombieInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getZombieInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotDeletedInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getReachableInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getReachableInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getRunningInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getRunningInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public List<InstanceMetaData> getNotDeletedAndNotZombieInstanceMetaDataList() {
        return new ArrayList<>(getNotDeletedAndNotZombieInstanceMetaDataSet());
    }

    public List<InstanceMetaData> getInstanceMetaDataAsList() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaData().stream())
                .collect(Collectors.toList());
    }

    public List<InstanceGroup> getInstanceGroupsAsList() {
        return new ArrayList<>(instanceGroups);
    }

    public boolean isStackInDeletionPhase() {
        return DELETE_COMPLETED.equals(getStatus()) || DELETE_IN_PROGRESS.equals(getStatus());
    }

    public boolean isStackInDeletionOrFailedPhase() {
        return isStackInDeletionPhase() || DELETE_FAILED.equals(getStatus());
    }

    public boolean isStopFailed() {
        return STOP_FAILED.equals(getStatus())
                || EXTERNAL_DATABASE_STOP_FAILED.equals(getStatus());
    }

    public boolean isStackInStopPhase() {
        return STOP_IN_PROGRESS.equals(getStatus()) || STOPPED.equals(getStatus())
                || EXTERNAL_DATABASE_STOP_IN_PROGRESS.equals(getStatus()) || EXTERNAL_DATABASE_STOP_FINISHED.equals(getStatus());
    }

    public boolean isStartFailed() {
        return START_FAILED.equals(getStatus())
                || EXTERNAL_DATABASE_START_FAILED.equals(getStatus());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<InstanceMetadataView> getNotTerminatedAndNotZombieGatewayInstanceMetadata() {
        return instanceGroups.stream()
                .filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType()))
                .flatMap(ig -> ig.getNotTerminatedAndNotZombieInstanceMetaDataSet().stream())
                .collect(Collectors.toList());
    }

    public List<InstanceMetaData> getNotTerminatedGatewayInstanceMetadata() {
        return instanceGroups.stream()
                .filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType()))
                .flatMap(ig -> ig.getNotTerminatedInstanceMetaDataSet().stream())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getReachableGatewayInstanceMetadata() {
        return instanceGroups.stream()
                .filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType()))
                .flatMap(ig -> ig.getReachableInstanceMetaDataSet().stream())
                .collect(Collectors.toList());
    }

    public InstanceMetadataView getPrimaryGatewayInstance() {
        Optional<InstanceMetadataView> metaData = getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType())).findFirst();
        return metaData.orElse(null);
    }

    public Optional<InstanceMetaData> getClusterManagerServer() {
        return getInstanceMetaDataAsList().stream()
                .filter(InstanceMetaData::getClusterManagerServer)
                .findFirst();
    }

    public Optional<InstanceGroup> getGatewayHostGroup() {
        return instanceGroups.stream()
                .filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType()))
                .findFirst();
    }

    public Network getNetwork() {
        return network;
    }

    @Override
    public List<InstanceGroupDto> getInstanceGroupDtos() {
        return instanceGroups.stream().map(ig -> new InstanceGroupDto(ig, ig.getAllAvailableInstanceMetadata())).collect(Collectors.toList());
    }

    @Override
    public Set<String> getAvailabilityZonesByInstanceGroup(Long instanceGroupId) {
        return instanceGroups.stream()
                .filter(ig -> ig.getId().equals(instanceGroupId))
                .findFirst()
                .orElseThrow(NotFoundException.notFound("InstanceGroup", instanceGroupId))
                .getAvailabilityZones();
    }

    @Override
    public GatewayView getGateway() {
        return cluster.getGateway();
    }

    @Override
    public Set<ClusterComponentView> getClusterComponents() {
        return cluster.getComponents().stream()
                .map(c -> {
                    ClusterComponentView cc = new ClusterComponentView();
                    cc.setClusterId(c.getCluster().getId());
                    cc.setComponentType(c.getComponentType());
                    cc.setName(c.getName());
                    cc.setAttributes(c.getAttributes());
                    cc.setId(c.getId());
                    return cc;
                })
                .collect(Collectors.toSet());
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public String getClusterManagerIp() {
        return cluster == null ? null : cluster.getClusterManagerIp();
    }

    public String getFqdn() {
        return cluster == null ? null : cluster.getFqdn();
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(getStatus());
    }

    public boolean isAvailableWithStoppedInstances() {
        // TODO CB-15146: This may need to change depending on the final form of how we check which operations are to be allowed
        //  when there are some STOPPED instances. The entire method may be removed.
        return isAvailable();
    }

    public boolean hasNodeFailure() {
        return NODE_FAILURE.equals(getStatus());
    }

    public boolean isStopRequested() {
        return STOP_REQUESTED.equals(getStatus());
    }

    public boolean isStopped() {
        return STOPPED.equals(getStatus());
    }

    public boolean isDeleteCompleted() {
        return DELETE_COMPLETED.equals(getStatus());
    }

    public boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS.equals(getStatus());
    }

    public boolean isStartInProgress() {
        return START_IN_PROGRESS.equals(getStatus()) || START_REQUESTED.equals(getStatus())
                || EXTERNAL_DATABASE_START_IN_PROGRESS.equals(getStatus()) || EXTERNAL_DATABASE_START_FINISHED.equals(getStatus());
    }

    public boolean isRequested() {
        return REQUESTED.equals(getStatus()) || CREATE_IN_PROGRESS.equals(getStatus());
    }

    public boolean isReadyForStop() {
        return AVAILABLE.equals(getStatus())
                || STOPPED.equals(getStatus())
                || STOP_REQUESTED.equals(getStatus())
                || STOP_IN_PROGRESS.equals(getStatus())
                || EXTERNAL_DATABASE_STOP_FINISHED.equals(getStatus())
                || NODE_FAILURE.equals(getStatus());
    }

    public boolean isExternalDatabaseStopped() {
        return EXTERNAL_DATABASE_STOP_FINISHED.equals(getStatus());
    }

    public boolean isModificationInProgress() {
        Status status = getStatus();
        return CREATE_IN_PROGRESS.equals(status)
                || UPDATE_IN_PROGRESS.equals(status)
                || STOP_IN_PROGRESS.equals(status)
                || EXTERNAL_DATABASE_STOP_IN_PROGRESS.equals(status)
                || START_IN_PROGRESS.equals(status)
                || EXTERNAL_DATABASE_START_IN_PROGRESS.equals(status)
                || DELETE_IN_PROGRESS.equals(status);
    }

    public boolean isMaintenanceModeEnabled() {
        return MAINTENANCE_MODE_ENABLED.equals(getStatus());
    }

    public boolean isStopInProgress() {
        return STOP_IN_PROGRESS.equals(getStatus()) || STOP_REQUESTED.equals(getStatus()) || EXTERNAL_DATABASE_STOP_IN_PROGRESS.equals(getStatus());
    }

    public boolean isReadyForStart() {
        return STOPPED.equals(getStatus()) || START_REQUESTED.equals(getStatus()) || START_IN_PROGRESS.equals(getStatus())
                || EXTERNAL_DATABASE_STOP_FINISHED.equals(getStatus()) || EXTERNAL_DATABASE_START_FINISHED.equals(getStatus());
    }

    public boolean isMultipleGateway() {
        int gatewayCount = 0;
        for (InstanceGroup ig : instanceGroups) {
            if (ig.getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                gatewayCount += ig.getNodeCount();
            }
        }
        return gatewayCount > 1;
    }

    public void populateStackIdForComponents() {
        components.stream().forEach(component -> {
            component.setStackId(id);
        });
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Set<Component> getComponents() {
        return components;
    }

    public boolean isInstanceGroupsSpecified() {
        return instanceGroups != null && !instanceGroups.isEmpty();
    }

    @Override
    public Json getTags() {
        return tags;
    }

    @Override
    public Long getClusterId() {
        return cluster == null ? null : cluster.getId();
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getCustomHostname() {
        return customHostname;
    }

    public void setCustomHostname(String customHostname) {
        this.customHostname = customHostname;
    }

    public boolean isHostgroupNameAsHostname() {
        return hostgroupNameAsHostname;
    }

    public void setHostgroupNameAsHostname(boolean hostgroupNameAsHostname) {
        this.hostgroupNameAsHostname = hostgroupNameAsHostname;
    }

    public boolean isClusterNameAsSubdomain() {
        return clusterNameAsSubdomain;
    }

    public void setClusterNameAsSubdomain(boolean clusterNameAsSubdomain) {
        this.clusterNameAsSubdomain = clusterNameAsSubdomain;
    }

    public StackAuthentication getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthentication stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public Json getInputs() {
        return inputs;
    }

    public void setInputs(Json inputs) {
        this.inputs = inputs;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public Tenant getTenant() {
        return WorkspaceAwareResource.super.getTenant();
    }

    public StackType getType() {
        return type;
    }

    @Override
    public List<InstanceMetadataView> getAllAvailableInstances() {
        return instanceGroups.stream()
                .flatMap(ig -> ig.getNotDeletedAndNotZombieInstanceMetaDataSet().stream())
                .collect(Collectors.toList());
    }

    public void setType(StackType type) {
        this.type = type;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public boolean isDatalake() {
        return type == StackType.DATALAKE;
    }

    public boolean hasCustomHostname() {
        return !StringUtils.isEmpty(customHostname) || hostgroupNameAsHostname;
    }

    public boolean isClusterProxyRegistered() {
        return clusterProxyRegistered;
    }

    public void setClusterProxyRegistered(boolean clusterProxyRegistered) {
        this.clusterProxyRegistered = clusterProxyRegistered;
    }

    public String getMinaSshdServiceId() {
        return minaSshdServiceId;
    }

    public void setMinaSshdServiceId(String minaSshdServiceId) {
        this.minaSshdServiceId = minaSshdServiceId;
    }

    public DatabaseAvailabilityType getExternalDatabaseCreationType() {
        return database.getExternalDatabaseAvailabilityType();
    }

    public Set<LoadBalancer> getLoadBalancers() {
        return loadBalancers;
    }

    public void setLoadBalancers(Set<LoadBalancer> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public List<TargetGroup> getTargetGroupAsList() {
        return loadBalancers.stream()
                .flatMap(loadBalancer -> loadBalancer.getTargetGroupSet().stream())
                .collect(Collectors.toList());
    }

    public String getCcmV2AgentCrn() {
        return ccmV2AgentCrn;
    }

    public void setCcmV2AgentCrn(String ccmV2AgentCrn) {
        this.ccmV2AgentCrn = ccmV2AgentCrn;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public String getExternalDatabaseEngineVersion() {
        return database.getExternalDatabaseEngineVersion();
    }

    @Override
    public Long getWorkspaceId() {
        return workspace.getId();
    }

    @Override
    public String getWorkspaceName() {
        return workspace.getName();
    }

    @Override
    public String getTenantName() {
        return getTenant().getName();
    }

    @Override
    public Long getTenantId() {
        return getTenant().getId();
    }

    @Override
    public String getResourceName() {
        return StackView.super.getResourceName();
    }

    public DnsResolverType getDomainDnsResolver() {
        return domainDnsResolver;
    }

    public void setDomainDnsResolver(DnsResolverType domainDnsResolver) {
        this.domainDnsResolver = domainDnsResolver;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Integer getJavaVersion() {
        return javaVersion;
    }

    @Override
    public boolean isMultiAz() {
        return multiAz;
    }

    @Override
    public Long getDatabaseId() {
        return Optional.ofNullable(database).map(Database::getId).orElse(null);
    }

    @Override
    public String getCreatorClient() {
        return creatorClient;
    }

    public void setJavaVersion(Integer javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setMultiAz(boolean multiAz) {
        this.multiAz = multiAz;
    }

    @Override
    public List<InstanceGroupView> getInstanceGroupViews() {
        return new ArrayList<>(instanceGroups);
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public String getSupportedImdsVersion() {
        return supportedImdsVersion;
    }

    public void setSupportedImdsVersion(String supportedImdsVersion) {
        this.supportedImdsVersion = supportedImdsVersion;
    }

    @Override
    public Architecture getArchitecture() {
        return architecture;
    }

    public String getArchitectureName() {
        return architecture == null ? null : architecture.getName();
    }

    public void setArchitecture(Architecture architecture) {
        this.architecture = architecture;
    }

    public Set<ProviderSyncState> getProviderSyncStates() {
        return providerSyncStates;
    }

    public void setProviderSyncStates(Set<ProviderSyncState> providerSyncStates) {
        this.providerSyncStates = providerSyncStates;
    }

    @Override
    public String toString() {
        return "Stack{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", gatewayPort=" + gatewayPort +
                ", useCcm=" + useCcm +
                ", tunnel=" + tunnel +
                ", consulServers=" + consulServers +
                ", customDomain='" + customDomain + '\'' +
                ", customHostname='" + customHostname + '\'' +
                ", hostgroupNameAsHostname=" + hostgroupNameAsHostname +
                ", clusterNameAsSubdomain=" + clusterNameAsSubdomain +
                ", displayName='" + displayName + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", stackVersion='" + stackVersion + '\'' +
                ", description='" + description + '\'' +
                ", parameters=" + parameters +
                ", platformVariant='" + platformVariant + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", cluster=" + cluster +
                ", stackStatus=" + stackStatus +
                ", onFailureActionAction=" + onFailureActionAction +
                ", failurePolicy=" + failurePolicy +
                ", securityConfig=" + securityConfig +
                ", instanceGroups=" + instanceGroups.stream().map(InstanceGroup::getGroupName).collect(Collectors.toSet()) +
                ", version=" + version +
                ", network=" + network +
                ", stackAuthentication=" + stackAuthentication +
                ", orchestrator=" + orchestrator +
                ", created=" + created +
                ", terminated=" + terminated +
                ", tags=" + tags +
                ", inputs=" + inputs +
                ", uuid='" + uuid + '\'' +
                ", workspace=" + workspace +
                ", creator=" + creator +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", datalakeCrn='" + datalakeCrn + '\'' +
                ", type=" + type +
                ", clusterProxyRegistered=" + clusterProxyRegistered +
                ", minaSshdServiceId='" + minaSshdServiceId + '\'' +
                ", ccmV2AgentCrn='" + ccmV2AgentCrn + '\'' +
                ", originalName=" + originalName +
                ", javaVersion=" + javaVersion +
                ", database=" + database +
                ", multiAz=" + multiAz +
                ", supportedImdsVersion=" + supportedImdsVersion +
                ", architecture=" + architecture +
                ", providerSyncStates=" + providerSyncStates +
                '}';
    }

    @Override
    public Set<Node> getAllFunctioningNodes() {
        return instanceGroups.stream()
                .flatMap(ig -> ig.getNotDeletedAndNotZombieInstanceMetaDataSet().stream())
                .filter(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()))
                .map(OrchestrationNode::getNode)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Node> getAllNotDeletedNodes() {
        return instanceGroups.stream()
                .flatMap(ig -> ig.getNotDeletedInstanceMetaDataSet().stream())
                .filter(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()))
                .map(OrchestrationNode::getNode)
                .collect(Collectors.toSet());
    }

    @Override
    public List<InstanceMetadataView> getAllAvailableGatewayInstances() {
        return new ArrayList<>(getNotTerminatedAndNotZombieGatewayInstanceMetadata());
    }
}
