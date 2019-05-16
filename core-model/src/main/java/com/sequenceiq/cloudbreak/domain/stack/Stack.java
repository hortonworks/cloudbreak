package com.sequenceiq.cloudbreak.domain.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Stack implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String region;

    private String availabilityZone;

    private Integer gatewayPort;

    private int consulServers;

    private String customDomain;

    private String customHostname;

    private boolean hostgroupNameAsHostname;

    private boolean clusterNameAsSubdomain;

    private String displayName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    private Map<String, String> parameters;

    @OneToOne
    private Credential credential;

    @Column(columnDefinition = "TEXT")
    private String platformVariant;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Cluster cluster;

    @OneToOne(cascade = CascadeType.ALL)
    private StackStatus stackStatus;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Resource> resources = new HashSet<>();

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OnFailureAction onFailureActionAction = OnFailureAction.ROLLBACK;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private FailurePolicy failurePolicy;

    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private SecurityConfig securityConfig;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
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

    private Long datalakeResourceId;

    @ManyToOne
    private Workspace workspace;

    @ManyToOne
    @JoinColumn(name = "createdBy")
    private User creator;

    @ManyToOne
    private EnvironmentView environment;

    @Enumerated(EnumType.STRING)
    private StackType type;

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
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

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
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

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }

    public List<Resource> getDiskResources() {
        switch (platformVariant) {
            case CloudConstants.AWS:
                return getResourcesByType(ResourceType.AWS_VOLUMESET);
            case CloudConstants.GCP:
                return getResourcesByType(ResourceType.GCP_ATTACHED_DISKSET);
            case CloudConstants.AZURE:
                return getResourcesByType(ResourceType.AZURE_VOLUMESET);
            default:
                return List.of();
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

    public InstanceGroup getInstanceGroupByInstanceGroupName(String group) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (group.equals(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public Integer getFullNodeCount() {
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            nodeCount += instanceGroup.getNodeCount();
        }
        return nodeCount;
    }

    public Set<InstanceMetaData> getNotTerminatedInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotTerminatedInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public List<InstanceMetaData> getNotTerminatedInstanceMetaDataList() {
        return new ArrayList<>(getNotTerminatedInstanceMetaDataSet());
    }

    public Set<InstanceMetaData> getNotDeletedInstanceMetaDataSet() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream())
                .collect(Collectors.toSet());
    }

    public List<InstanceMetaData> getNotDeletedInstanceMetaDataList() {
        return new ArrayList<>(getNotDeletedInstanceMetaDataSet());
    }

    public List<InstanceMetaData> getInstanceMetaDataAsList() {
        return instanceGroups.stream()
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream())
                .collect(Collectors.toList());
    }

    public List<InstanceGroup> getInstanceGroupsAsList() {
        return new ArrayList<>(instanceGroups);
    }

    public boolean isStackInDeletionPhase() {
        return DELETE_COMPLETED.equals(getStatus()) || DELETE_IN_PROGRESS.equals(getStatus());
    }

    public boolean isStopFailed() {
        return STOP_FAILED.equals(getStatus());
    }

    public boolean isStackInStopPhase() {
        return STOP_IN_PROGRESS.equals(getStatus()) || STOPPED.equals(getStatus());
    }

    public boolean isStartFailed() {
        return START_FAILED.equals(getStatus());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<InstanceMetaData> getGatewayInstanceMetadata() {
        List<InstanceMetaData> metadataList = new ArrayList<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                metadataList.addAll(instanceGroup.getNotTerminatedInstanceMetaDataSet());
            }
        }
        return metadataList;
    }

    public InstanceMetaData getPrimaryGatewayInstance() {
        Optional<InstanceMetaData> metaData = getGatewayInstanceMetadata().stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType())).findFirst();
        return metaData.orElse(null);
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public String getAmbariIp() {
        return cluster == null ? null : cluster.getAmbariIp();
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(getStatus());
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
        return START_IN_PROGRESS.equals(getStatus()) || START_REQUESTED.equals(getStatus());
    }

    public boolean isRequested() {
        return REQUESTED.equals(getStatus()) || CREATE_IN_PROGRESS.equals(getStatus());
    }

    public boolean isStackReadyForStop() {
        return AVAILABLE.equals(getStatus()) || STOP_REQUESTED.equals(getStatus());
    }

    public boolean isModificationInProgress() {
        Status status = getStatus();
        return CREATE_IN_PROGRESS.equals(status)
                || UPDATE_IN_PROGRESS.equals(status)
                || STOP_IN_PROGRESS.equals(status)
                || START_IN_PROGRESS.equals(status)
                || DELETE_IN_PROGRESS.equals(status);
    }

    public StopRestrictionReason isInfrastructureStoppable() {
        StopRestrictionReason reason = StopRestrictionReason.NONE;
        if ("AWS".equals(cloudPlatform())) {
            for (InstanceGroup instanceGroup : instanceGroups) {
                if (instanceGroup.getTemplate().getVolumeTemplates().stream()
                        .filter(volume -> "ephemeral".equals(volume.getVolumeType())).findAny().isPresent()) {
                    reason = StopRestrictionReason.EPHEMERAL_VOLUMES;
                    break;
                }
            }
        }
        return reason;
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

    public void setComponents(Set<Component> components) {
        this.components = components;
    }

    public boolean isInstanceGroupsSpecified() {
        return instanceGroups != null && !instanceGroups.isEmpty();
    }

    public Json getTags() {
        return tags;
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

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
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

    public Long getDatalakeResourceId() {
        return datalakeResourceId;
    }

    public void setDatalakeResourceId(Long datalakeResourceId) {
        this.datalakeResourceId = datalakeResourceId;
    }

    public boolean isDatalake() {
        return type == StackType.DATALAKE;
    }
}
