package com.sequenceiq.cloudbreak.dto;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.domain.IdAware;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
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

public class StackDto implements OrchestratorAware, StackDtoDelegate, MdcContextInfoProvider, IdAware {

    private StackView stack;

    private ClusterView cluster;

    private Network network;

    private Workspace workspace;

    private Tenant tenant;

    private Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();

    private Set<Resource> resources;

    private Blueprint blueprint;

    private GatewayView gateway;

    private Orchestrator orchestrator;

    private FileSystem fileSystem;

    private FileSystem additionalFileSystem;

    private Set<ClusterComponentView> clusterComponents;

    private List<StackParameters> stackParameters;

    private SecurityConfig securityConfig;

    private Map<InstanceGroupView, List<String>> availabilityZonesByInstanceGroup;

    public StackDto(StackView stack, ClusterView cluster, Network network, Workspace workspace, Tenant tenant, Map<String, InstanceGroupDto> instanceGroups,
            Set<Resource> resources, Blueprint blueprint, GatewayView gateway, Orchestrator orchestrator,
            FileSystem fileSystem, FileSystem additionalFileSystem, Set<ClusterComponentView> clusterComponents, List<StackParameters> stackParameters,
            SecurityConfig securityConfig, Map<InstanceGroupView, List<String>> availabilityZonesByInstanceGroup) {
        this.stack = stack;
        this.cluster = cluster;
        this.network = network;
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
    }

    @VisibleForTesting
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

    public List<InstanceMetadataView> getNotTerminatedAndNotZombieGatewayInstanceMetadata() {
        return getAllAvailableInstances().stream()
                .filter(im -> im.getInstanceGroupType() == InstanceGroupType.GATEWAY)
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

    public Set<Node> getAllNodes() {
        Set<Node> ret = new HashSet<>();
        getInstanceGroupDtos().forEach(ig -> {
            InstanceGroupView instanceGroup = ig.getInstanceGroup();
            ig.getInstanceMetadataViews().forEach(im -> {
                if (StringUtils.isNotBlank(im.getDiscoveryFQDN())) {
                    ret.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                            instanceGroup.getTemplate().getInstanceType(), im.getDiscoveryFQDN(), instanceGroup.getGroupName()));
                }
            });
        });
        return ret;
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
        return getAllAvailableInstances().stream()
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
                .filter(imd -> imd.isGatewayOrPrimaryGateway())
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
}
