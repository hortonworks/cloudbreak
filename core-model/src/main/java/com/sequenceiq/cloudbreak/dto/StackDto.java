package com.sequenceiq.cloudbreak.dto;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.domain.IdAware;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;

public class StackDto implements OrchestratorAware, StackDtoDelegate, MdcContextInfoProvider, IdAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDto.class);

    private StackView stack;

    private ClusterView cluster;

    private Network network;

    private Database database;

    private Workspace workspace;

    private Tenant tenant;

    private Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();

    private Set<Resource> resources;

    private Blueprint blueprint;

    private GatewayView gateway;

    private Orchestrator orchestrator;

    private Architecture architecture;

    private FileSystem fileSystem;

    private FileSystem additionalFileSystem;

    private Set<ClusterComponentView> clusterComponents;

    private List<StackParameters> stackParameters;

    private SecurityConfig securityConfig;

    private Map<InstanceGroupView, List<String>> availabilityZonesByInstanceGroup;

    public StackDto(StackView stack, ClusterView cluster, Network network, Database database, Workspace workspace, Tenant tenant,
            Map<String, InstanceGroupDto> instanceGroups, Set<Resource> resources, Blueprint blueprint, GatewayView gateway, Orchestrator orchestrator,
            FileSystem fileSystem, FileSystem additionalFileSystem, Set<ClusterComponentView> clusterComponents, Architecture architecture,
            List<StackParameters> stackParameters, SecurityConfig securityConfig, Map<InstanceGroupView, List<String>> availabilityZonesByInstanceGroup) {
        this.stack = stack;
        this.cluster = cluster;
        this.network = network;
        this.database = database;
        this.workspace = workspace;
        this.instanceGroups = instanceGroups;
        this.resources = resources;
        this.blueprint = blueprint;
        this.gateway = gateway;
        this.orchestrator = orchestrator;
        this.fileSystem = fileSystem;
        this.additionalFileSystem = additionalFileSystem;
        this.clusterComponents = clusterComponents;
        this.stackParameters = stackParameters;
        this.securityConfig = securityConfig;
        this.availabilityZonesByInstanceGroup = availabilityZonesByInstanceGroup;
        this.tenant = tenant;
        this.architecture = architecture;
    }

    public StackDto() {
    }

    @Override
    public Long getId() {
        return getStack().getId();
    }

    public Set<ClusterComponentView> getClusterComponents() {
        return clusterComponents;
    }

    public StackView getStack() {
        return stack;
    }

    public List<InstanceGroupDto> getInstanceGroupDtos() {
        return new ArrayList<>(instanceGroups.values());
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getWorkspaceName() {
        return getWorkspace().getName();
    }

    public Long getWorkspaceId() {
        return getStack().getWorkspaceId();
    }

    @Override
    public String getResourceName() {
        return getName();
    }

    @Override
    public String getResourceType() {
        return "STACK";
    }

    @Override
    public String getTenantName() {
        return getStack().getTenantName();
    }

    @Override
    public String getResourceCrn() {
        return getStack().getResourceCrn();
    }

    @Override
    public String getEnvironmentCrn() {
        return getStack().getEnvironmentCrn();
    }

    public ClusterView getCluster() {
        return cluster;
    }

    public Network getNetwork() {
        return network;
    }

    public Database getDatabase() {
        return database;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public Set<Resource> getResources() {
        if (resources == null) {
            throw new NotImplementedException("Resource fetch are disabled");
        }
        return resources;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public GatewayView getGateway() {
        return gateway;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public FileSystem getAdditionalFileSystem() {
        return additionalFileSystem;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public String getAccountId() {
        return Crn.safeFromString(getResourceCrn()).getAccountId();
    }

    @Override
    public InstanceGroupDto getInstanceGroupByInstanceGroupName(String instanceGroup) {
        return instanceGroups.get(instanceGroup);
    }

    @Override
    public List<InstanceMetadataView> getAllAvailableGatewayInstances() {
        return getAllAvailableInstances().stream()
                .filter(im -> InstanceGroupType.GATEWAY.equals(im.getInstanceGroupType()))
                .collect(Collectors.toList());
    }

    public Map<InstanceGroupView, List<String>> getAvailabilityZonesByInstanceGroup() {
        return availabilityZonesByInstanceGroup;
    }

    public Map<String, String> getParameters() {
        return stackParameters.stream().collect(Collectors.toMap(StackParameters::getKey, StackParameters::getValue));
    }

    public List<InstanceMetadataView> getAllNotTerminatedInstanceMetaData() {
        return instanceGroups.values().stream()
                .flatMap(ig -> ig.getInstanceMetadataViews().stream())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getAllAvailableInstances() {
        return instanceGroups.values().stream()
                .flatMap(ig -> ig.getNotDeletedAndNotZombieInstanceMetaData().stream())
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getNotDeletedInstanceMetaData() {
        return instanceGroups.entrySet().stream()
                .flatMap(e -> e.getValue().getNotDeletedInstanceMetaData().stream())
                .collect(Collectors.toList());
    }

    public InstanceMetadataView getPrimaryGatewayInstance() {
        Optional<InstanceMetadataView> metaData = getAllAvailableInstances().stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType())).findFirst();
        return metaData.orElse(null);
    }

    public Optional<String> getPrimaryGatewayFQDN() {
        return getAllAvailableInstances().stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType()))
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(StringUtils::isNotBlank)
                .findFirst();
    }

    public Set<String> getSecondaryGatewayFQDNs() {
        return getAllAvailableInstances().stream()
                .filter(im -> InstanceMetadataType.GATEWAY.equals(im.getInstanceMetadataType()))
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    public List<InstanceMetadataView> getNotTerminatedAndNotZombieGatewayInstanceMetadata() {
        return getAllAvailableInstances().stream()
                .filter(im -> im.getInstanceGroupType() == InstanceGroupType.GATEWAY)
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getNotTerminatedGatewayInstanceMetadata() {
        return instanceGroups.values().stream()
                .flatMap(ig -> ig.getInstanceMetadataViews().stream())
                .filter(im -> im.getInstanceGroupType() == InstanceGroupType.GATEWAY)
                .filter(im -> !im.isTerminated())
                .collect(Collectors.toList());
    }

    public List<InstanceGroupDto> getNotTerminatedAndNotZombieGatewayInstanceMetadataWithInstanceGroup() {
        List<InstanceGroupDto> ret = new ArrayList<>();
        instanceGroups.values().forEach(ig -> {
            if (ig.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                ret.add(ig);
            }
        });
        return ret;
    }

    public Long getFullNodeCount() {
        return getAllAvailableInstances().stream().count();
    }

    public Set<InstanceMetadataView> getRunningInstanceMetaDataSet() {
        return instanceGroups.values().stream()
                .flatMap(ig -> ig.getRunningInstanceMetaData().stream())
                .collect(Collectors.toSet());
    }

    public List<InstanceMetadataView> getReachableGatewayInstanceMetadata() {
        List<InstanceMetadataView> gatewayInstanceMetadataViews = new ArrayList<>();
        instanceGroups.values().forEach(ig -> {
            if (ig.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                gatewayInstanceMetadataViews.addAll(ig.getReachableInstanceMetaData());
            }
        });
        return gatewayInstanceMetadataViews;
    }

    public List<InstanceMetadataView> getNotDeletedGatewayInstanceMetaData() {
        List<InstanceMetadataView> gatewayInstanceMetadataViews = new ArrayList<>();
        instanceGroups.values().forEach(ig -> {
            if (ig.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                gatewayInstanceMetadataViews.addAll(ig.getNotDeletedInstanceMetaData());
            }
        });
        return gatewayInstanceMetadataViews;
    }

    public List<InstanceGroupDto> getReachableInstanceMetaDataSetWithInstanceGroup() {
        List<InstanceGroupDto> ret = new ArrayList<>();
        instanceGroups.values().forEach(ig -> {
            List<InstanceMetadataView> filteredIms = ig.getReachableInstanceMetaData();
            ret.add(new InstanceGroupDto(ig.getInstanceGroup(), filteredIms));
        });
        return ret;
    }

    public boolean hasGateway() {
        return gateway != null;
    }

    @Override
    public Set<Node> getAllFunctioningNodes() {
        return getNodesByMetaDataFilter(InstanceGroupDto::getNotDeletedAndNotZombieInstanceMetaData);
    }

    @Override
    public Set<Node> getAllNotDeletedNodes() {
        return getNodesByMetaDataFilter(InstanceGroupDto::getNotDeletedInstanceMetaData);
    }

    private Set<Node> getNodesByMetaDataFilter(Function<InstanceGroupDto, List<InstanceMetadataView>> metadataFilter) {
        Map<InstanceMetadataView, InstanceGroupView> instanceMetadataMap = mapFilteredInstanceMetadatasWithFQDNToGroup(metadataFilter);
        Map<InstanceMetadataView, InstanceGroupView> deduplicatedByPrivateIp = deduplicateByPrivateIp(instanceMetadataMap);
        return deduplicatedByPrivateIp.entrySet().stream().map(entry -> {
            InstanceMetadataView im = entry.getKey();
            InstanceGroupView instanceGroup = entry.getValue();
            return new Node(im.getPrivateIp(),
                    im.getPublicIp(),
                    im.getInstanceId(),
                    instanceGroup.getTemplate().getInstanceType(),
                    im.getDiscoveryFQDN(),
                    instanceGroup.getGroupName());
        }).collect(Collectors.toSet());
    }

    private Map<InstanceMetadataView, InstanceGroupView> deduplicateByPrivateIp(Map<InstanceMetadataView, InstanceGroupView> instanceMetadataMap) {
        return instanceMetadataMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getPrivateIp(), entry -> entry, (i1, i2) -> {
                    LOGGER.warn("We have the same ip address for two nodes, we will return with the newer node! Affected nodes: {}, {}", i1, i2);
                    if (i1.getKey().getStartDate().compareTo(i2.getKey().getStartDate()) < 0) {
                        return i2;
                    } else {
                        return i1;
                    }
                })).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getKey(), entry -> entry.getValue().getValue()));
    }

    private Map<InstanceMetadataView, InstanceGroupView> mapFilteredInstanceMetadatasWithFQDNToGroup(
            Function<InstanceGroupDto, List<InstanceMetadataView>> metadataFilter) {
        return getInstanceGroupDtos().stream()
                .flatMap(dto -> metadataFilter.apply(dto).stream()
                        .map(metadata -> new SimpleImmutableEntry<>(dto.getInstanceGroup(), metadata)))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue().getDiscoveryFQDN()))
                .collect(Collectors.toMap(SimpleImmutableEntry::getValue, SimpleImmutableEntry::getKey));
    }

    public Optional<InstanceGroupView> getGatewayGroup() {
        return instanceGroups.values().stream().filter(ig -> ig.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY)
                .findFirst().map(ig -> ig.getInstanceGroup());
    }

    public InstanceGroupView getPrimaryGatewayGroup() {
        return instanceGroups.values().stream()
                .filter(ig -> ig.getInstanceMetadataViews().stream().anyMatch(im -> im.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY))
                .findFirst()
                .orElseThrow(notFound("Gateway Instance Group for Stack", stack.getId()))
                .getInstanceGroup();
    }

    public Optional<InstanceMetadataView> getInstanceMetadata(Long privateId) {
        return getAllNotTerminatedInstanceMetaData().stream()
                .filter(instanceMetaData -> privateId.equals(instanceMetaData.getPrivateId()))
                .findFirst();
    }

    public Optional<InstanceMetadataView> getNotDeletedInstanceMetadata(Long privateId) {
        return getNotDeletedInstanceMetaData().stream()
                .filter(instanceMetaData -> privateId.equals(instanceMetaData.getPrivateId()))
                .findFirst();
    }

    public List<String> getInstanceIdsForPrivateIds(Set<Long> privateIds) {
        List<InstanceMetadataView> instanceMetaDataForPrivateIds = getInstanceMetaDataForPrivateIds(privateIds);
        return instanceMetaDataForPrivateIds.stream()
                .map(instanceMetaData -> {
                    if (instanceMetaData.getInstanceId() != null) {
                        return instanceMetaData.getInstanceId();
                    } else {
                        return instanceMetaData.getPrivateId().toString();
                    }
                })
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getInstanceMetaDataForPrivateIds(Collection<Long> privateIds) {
        return getAllAvailableInstances().stream()
                .filter(instanceMetaData -> privateIds.contains(instanceMetaData.getPrivateId()))
                .collect(Collectors.toList());
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Set<String> getAvailabilityZonesByInstanceGroup(Long igId) {
        return availabilityZonesByInstanceGroup.entrySet().stream()
                .filter(e -> e.getKey().getId().equals(igId))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet());
    }

    public boolean hasCustomHostname() {
        return !StringUtils.isEmpty(getStack().getCustomHostname()) || getStack().isHostgroupNameAsHostname();
    }

    public List<InstanceGroupView> getInstanceGroupViews() {
        return getInstanceGroupDtos().stream().map(InstanceGroupDto::getInstanceGroup).collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getAllPrimaryGatewayInstances() {
        return getNotDeletedInstanceMetaData().stream()
                .filter(InstanceMetadataView::isGatewayOrPrimaryGateway)
                .collect(Collectors.toList());
    }

    public List<InstanceMetadataView> getZombieInstanceMetaData() {
        return getNotDeletedInstanceMetaData().stream()
                .filter(metaData -> metaData.isZombie())
                .collect(Collectors.toList());
    }

    public boolean isAvailable() {
        return getStack().isAvailable();
    }

    public Collection<InstanceMetadataView> getUnusedHostsInInstanceGroup(String instanceGroupName) {
        return getNotDeletedInstanceMetaData().stream()
                .filter(im -> im.isCreated() && im.getInstanceGroupName().equals(instanceGroupName))
                .collect(Collectors.toList());
    }

    public Collection<InstanceMetadataView> getAliveInstancesInInstanceGroup(String instanceGroupName) {
        return getAllAvailableInstances().stream()
                .filter(im -> im.getInstanceGroupName().equals(instanceGroupName))
                .collect(Collectors.toList());
    }

    public Set<Node> getAllPrimaryGatewayInstanceNodes() {
        Set<Node> ret = new HashSet<>();
        getInstanceGroupDtos().forEach(ig -> {
            InstanceGroupView instanceGroup = ig.getInstanceGroup();
            ig.getInstanceMetadataViews().stream()
                    .filter(InstanceMetadataView::isGatewayOrPrimaryGateway)
                    .forEach(im -> {
                        ret.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                                instanceGroup.getTemplate().getInstanceType(), im.getDiscoveryFQDN(), instanceGroup.getGroupName()));
                    });
        });
        return ret;
    }

    public boolean isStackInStopPhase() {
        return STOP_IN_PROGRESS.equals(getStatus()) || STOPPED.equals(getStatus());
    }

    public boolean isOnGovPlatformVariant() {
        return CloudConstants.AWS_NATIVE_GOV.equals(getPlatformVariant());
    }

    @Override
    public Json getTags() {
        return getStack().getTags();
    }

    public StackTags getStackTags() {
        if (getStack().getTags() != null) {
            try {
                return stack.getTags().get(StackTags.class);
            } catch (IOException e) {
                LOGGER.warn("Stack related tags cannot be parsed.", e);
            }
        }
        return null;
    }
}
