package com.sequenceiq.cloudbreak.service.stack;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.ClusterDtoRepository;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.repository.StackParametersRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.view.AvailabilityZoneView;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class StackDtoService {

    private static final Logger LOGGER = getLogger(StackDtoService.class);

    private static final List<ComponentType> COMPONENT_TYPES_TO_FETCH = List.of(ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CM_REPO_DETAILS);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackDtoRepository stackDtoRepository;

    @Inject
    private ClusterDtoRepository clusterDtoRepository;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private OrchestratorService orchestratorService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private StackParametersRepository stackParametersRepository;

    public StackDto getByNameOrCrn(NameOrCrn nameOrCrn, String accountId, StackType stackType,
            ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        StackView stackView = nameOrCrn.hasName() ?
                getByName(accountId, nameOrCrn.getName(), stackType, config) :
                getByCrn(nameOrCrn.getCrn(), stackType, config);
        return getStackProxy(stackView, false);
    }

    public StackDto getByNameOrCrn(NameOrCrn nameOrCrn, String accountId) {
        StackView stackView = nameOrCrn.hasName() ?
                stackDtoRepository.findByName(accountId, nameOrCrn.getName()).orElseThrow(NotFoundException.notFound("Stack by name", nameOrCrn.getName())) :
                stackDtoRepository.findByCrn(nameOrCrn.getCrn()).orElseThrow(NotFoundException.notFound("Stack by crn", nameOrCrn.getCrn()));
        return getStackProxy(stackView, false);
    }

    public StackDto getById(Long id) {
        return getById(id, true);
    }

    public StackDto getById(Long id, boolean fetchResources) {
        StackView stackView = stackDtoRepository.findById(id).orElseThrow(NotFoundException.notFound("Stack", id));
        return getStackProxy(stackView, fetchResources);
    }

    public Optional<StackDto> getByIdOpt(Long id) {
        return stackDtoRepository.findById(id).map(stackViewDelegate -> getStackProxy(stackViewDelegate, false));
    }

    public StackDto getByCrn(String crn) {
        StackView stackView = stackDtoRepository.findByCrn(crn).orElseThrow(NotFoundException.notFound("Stack by crn", crn));
        return getStackProxy(stackView, false);
    }

    private StackDto getStackProxy(StackView stackView, boolean fetchResources) {
        Map<String, InstanceGroupDto> groupListMap = new HashMap<>();
        List<InstanceMetadataView> imDto = instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stackView.getId());
        Map<Long, Map<InstanceGroupView, List<InstanceMetadataView>>> group = new HashMap<>();
        List<InstanceGroupView> instanceGroups = instanceGroupService.getInstanceGroupViewByStackId(stackView.getId());
        instanceGroups.forEach(it -> group.put(it.getId(), new HashMap<>()));
        List<String> groupString = instanceGroups.stream()
                .map(ig -> String.format("%s: %s, %s, %s, %s",
                        ig.getId(),
                        ig.getGroupName(),
                        ig.getSecurityGroup(),
                        ig.getTemplate(),
                        ig.getInstanceGroupNetwork()))
                .collect(Collectors.toList());
        LOGGER.debug("Fetched groups: {} by stack: {}", Joiner.on(",").join(groupString), stackView.getId());
        List<String> instanceMetadataString = imDto.stream()
                .map(im -> String.format("The %s with id: %s add to group of %s with id: %s",
                        im.getInstanceName(),
                        im.getId(),
                        im.getInstanceGroupName(),
                        im.getInstanceGroupId()))
                .collect(Collectors.toList());
        LOGGER.debug("Fetched instance metadata: {} by stack: {}", Joiner.on(",").join(instanceMetadataString), stackView.getId());
        imDto.forEach(im -> {
            var imByIg = group.computeIfAbsent(im.getInstanceGroupId(), key -> new HashMap<>());
            if (imByIg.isEmpty()) {
                imByIg.put(instanceGroups.stream().filter(ig -> ig.getId().equals(im.getInstanceGroupId())).findFirst().get(), new ArrayList<>());
            }
            imByIg.values().stream().findFirst().get().add(im);
        });
        group.forEach((id, map) -> {
            if (map.isEmpty()) {
                InstanceGroupView instanceGroupView = instanceGroups.stream().filter(ig -> ig.getId().equals(id)).findFirst().get();
                groupListMap.put(instanceGroupView.getGroupName(), new InstanceGroupDto(instanceGroupView, new ArrayList<>()));
            } else {
                InstanceGroupView instanceGroupView = map.keySet().stream().findFirst().get();
                List<InstanceMetadataView> instanceMetadataViews = map.values().stream().findFirst().orElse(new ArrayList<>());
                groupListMap.put(instanceGroupView.getGroupName(), new InstanceGroupDto(instanceGroupView, instanceMetadataViews));
            }
        });
        Set<Resource> resources = null;
        if (fetchResources) {
            resources = new HashSet<>(resourceService.getAllByStackId(stackView.getId()));
        }
        ClusterView cluster = clusterDtoRepository.findByStackId(stackView.getId()).orElse(null);
        Network network = stackDtoRepository.getNetworkByStackById(stackView.getId()).orElse(null);
        Workspace workspace = workspaceService.getByIdWithoutAuth(stackView.getWorkspaceId());
        Blueprint blueprint = null;
        GatewayView gateway = null;
        Orchestrator orchestrator = null;
        FileSystem fileSystem = null;
        FileSystem additionalFileSystem = null;
        Set<ClusterComponentView> components = null;
        if (cluster != null) {
            blueprint = blueprintService.getByClusterId(cluster.getId()).orElse(null);
            gateway = gatewayService.getByClusterId(cluster.getId()).orElse(null);
            orchestrator = orchestratorService.getByStackId(stackView.getId()).orElse(null);
            fileSystem = cluster.getFileSystem();
            additionalFileSystem = cluster.getAdditionalFileSystem();
            components = clusterComponentConfigProvider.getComponentsByClusterIdAndInComponentType(cluster.getId(), COMPONENT_TYPES_TO_FETCH);
        }
        List<StackParameters> parameters = stackParametersRepository.findAllByStackId(stackView.getId());
        SecurityConfig securityConfig = stackDtoRepository.getSecurityByStackId(stackView.getId());
        Map<InstanceGroupView, List<String>> availabilityZonesByStackId = new HashMap<>();
        if (!instanceGroups.isEmpty()) {
            availabilityZonesByStackId = instanceGroupService.getAvailabilityZonesByStackId(stackView.getId())
                    .entrySet().stream()
                    .collect(
                            Collectors.toMap(
                                    e -> instanceGroups.stream().filter(ig -> ig.getId().equals(e.getKey())).findFirst().get(),
                                    e -> e.getValue().stream().map(AvailabilityZoneView::getAvailabilityZone).collect(Collectors.toList())));
        }
        return new StackDto(stackView, cluster, network, workspace, workspace.getTenant(), groupListMap, resources, blueprint, gateway,
                orchestrator, fileSystem, additionalFileSystem, components, parameters, securityConfig, availabilityZonesByStackId);
    }

    public List<InstanceGroupDto> getInstanceMetadataByInstanceGroup(Long stackId) {
        List<InstanceGroupDto> groupListMap = new ArrayList<>();
        List<InstanceMetadataView> imDto = instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stackId);
        Map<Long, Map<InstanceGroupView, List<InstanceMetadataView>>> group = new HashMap<>();
        List<InstanceGroupView> instanceGroups = instanceGroupService.getInstanceGroupViewByStackId(stackId);
        instanceGroups.forEach(it -> group.put(it.getId(), new HashMap<>()));
        imDto.forEach(im -> {
            var imByIg = group.computeIfAbsent(im.getInstanceGroupId(), key -> new HashMap<>());
            if (imByIg.isEmpty()) {
                imByIg.put(instanceGroups.stream().filter(ig -> ig.getId().equals(im.getInstanceGroupId())).findFirst().get(), new ArrayList<>());
            }
            imByIg.values().stream().findFirst().get().add(im);
        });
        group.forEach((id, map) -> {
            if (map.isEmpty()) {
                InstanceGroupView instanceGroupView = instanceGroups.stream().filter(ig -> ig.getId().equals(id)).findFirst().get();
                groupListMap.add(new InstanceGroupDto(instanceGroupView, new ArrayList<>()));
            } else {
                InstanceGroupView instanceGroupView = map.keySet().stream().findFirst().get();
                List<InstanceMetadataView> instanceMetadataViews = map.values().stream().findFirst().orElse(new ArrayList<>());
                groupListMap.add(new InstanceGroupDto(instanceGroupView, instanceMetadataViews));
            }
        });
        return groupListMap;
    }

    public StackView getStackViewByNameOrCrn(NameOrCrn nameOrCrn, String accountId) {
        return nameOrCrn.hasName() ? stackDtoRepository.findByName(accountId, nameOrCrn.getName())
                .orElseThrow(NotFoundException.notFound("Stack by name", nameOrCrn.getName())) :
                stackDtoRepository.findByCrn(nameOrCrn.getCrn())
                        .orElseThrow(NotFoundException.notFound("Stack by crn", nameOrCrn.getCrn()));
    }

    public Optional<StackView> getStackViewByNameOrCrnOpt(NameOrCrn nameOrCrn, String accountId) {
        Optional<StackViewDelegate> stackViewDelegate = nameOrCrn.hasName() ?
                stackDtoRepository.findByName(accountId, nameOrCrn.getName()) :
                stackDtoRepository.findByCrn(nameOrCrn.getCrn());
        return Optional.ofNullable(stackViewDelegate.orElse(null));
    }

    public StackView getStackViewByName(String name, String accountId) {
        return stackDtoRepository.findByName(accountId, name).orElseThrow(NotFoundException.notFound("Stack by name", name));
    }

    public StackView getStackViewByCrn(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackDtoRepository.findByCrn(crn);
        return stackViewDelegate.orElseThrow(NotFoundException.notFound("Stack by crn", crn));
    }

    public StackView getStackViewById(Long id) {
        return stackDtoRepository.findById(id).orElseThrow(NotFoundException.notFound("Stack", id));
    }

    public Optional<StackView> getStackViewByIdOpt(Long id) {
        return stackDtoRepository.findById(id).map(StackView.class::cast);
    }

    public ClusterView getClusterViewByStackId(Long id) {
        return clusterDtoRepository.findByStackId(id).orElse(null);
    }

    public StackView getByName(String accountId, String name, StackType stackType,
            ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        return stackType == null ?
                stackDtoRepository.findByName(accountId, name, config.isActive(),
                        config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", name)) :
                stackDtoRepository.findByName(accountId, name, stackType, config.isActive(),
                        config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", name));
    }

    public StackView getByCrn(String crn, StackType stackType, ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        return stackType == null ?
                stackDtoRepository.findByCrn(crn, config.isActive(), config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", crn)) :
                stackDtoRepository.findByCrn(crn, stackType, config.isActive(), config.showAfterMillisecs())
                        .orElseThrow(NotFoundException.notFound("Stack", crn));
    }

    public List<StackParameters> getStackParameters(Long stackId) {
        return stackParametersRepository.findAllByStackId(stackId);
    }

    public void updateDomainDnsResolver(Long stackId, DnsResolverType actualDnsResolverType) {
        stackService.updateDomainDnsResolverByStackId(stackId, actualDnsResolverType);
    }

    public GatewayView getGatewayView(Long clusterId) {
        return gatewayService.getByClusterId(clusterId).orElse(null);
    }

    public Blueprint getBlueprint(Long clusterId) {
        return blueprintService.getByClusterId(clusterId).orElse(null);
    }

    public SecurityConfig getSecurityConfig(Long stackId) {
        return stackDtoRepository.getSecurityByStackId(stackId);
    }

    public Boolean hasGateway(Long clusterId) {
        return gatewayService.existsByClusterId(clusterId);
    }

    public Stack getStackReferenceById(Long stackId) {
        return stackService.getStackReferenceById(stackId);
    }

    public List<StackView> findNotTerminatedByCrns(Collection<String> resourceCrns) {
        return new ArrayList<>(stackDtoRepository.findAllByResourceCrnIn(resourceCrns));
    }

    public List<StackView> findNotTerminatedByResourceCrns(Collection<String> resourceCrns) {
        return new ArrayList<>(stackDtoRepository.findNotTerminatedByResourceCrnIn(resourceCrns));
    }

    public List<StackView> findNotTerminatedByResourceCrnsAndCloudPlatforms(Collection<String> resourceCrns, Collection<CloudPlatform> cloudPlatforms) {
        return new ArrayList<>(stackDtoRepository.findNotTerminatedByResourceCrnsAndCloudPlatforms(resourceCrns,
                cloudPlatforms.stream().map(CloudPlatform::name).collect(Collectors.toList())));
    }

    public List<StackView> findNotTerminatedByEnvironmentCrnsAndCloudPlatforms(Collection<String> environmentCrns, Collection<CloudPlatform> cloudPlatforms) {
        return new ArrayList<>(stackDtoRepository.findNotTerminatedByEnvironmentCrnsAndCloudPlatforms(environmentCrns,
                cloudPlatforms.stream().map(CloudPlatform::name).collect(Collectors.toList())));
    }

    public Optional<StackView> findNotTerminatedByCrn(String resourceCrn) {
        Optional<StackViewDelegate> stackView = stackDtoRepository.findByResourceCrn(resourceCrn);
        return Optional.ofNullable(stackView.orElse(null));
    }

    public List<StackView> findNotTerminatedByNamesAndAccountId(List<String> resourceNames, String accountId) {
        return new ArrayList<>(stackDtoRepository.findAllByNamesAndAccountId(resourceNames, accountId));
    }

    public Optional<StackView> findNotTerminatedByNameAndAccountId(String resourceName, String accountId) {
        Optional<StackViewDelegate> stackView = stackDtoRepository.findByNameAndAccountId(resourceName, accountId);
        return Optional.ofNullable(stackView.orElse(null));
    }

    public List<StackDto> findAllByEnvironmentCrnAndStackType(String environmentCrn, List<StackType> stackTypes) {
        Objects.requireNonNull(environmentCrn);
        Objects.requireNonNull(stackTypes);
        return stackDtoRepository.findAllByEnvironmentCrnAndStackType(environmentCrn, stackTypes).stream()
                .map(stackViewDelegate -> getStackProxy(stackViewDelegate, false))
                .collect(Collectors.toList());
    }
}
