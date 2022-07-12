package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.StackParametersRepository;
import com.sequenceiq.cloudbreak.repository.view.ClusterViewRepository;
import com.sequenceiq.cloudbreak.repository.view.StackViewRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.CommonStatus;

@Component
public class StackDtoService {

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackViewRepository stackViewRepository;

    @Inject
    private ClusterViewRepository clusterViewRepository;

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
        return getStackProxy(stackView, false).orElseThrow(NotFoundException.notFound("StackDto", nameOrCrn.getNameOrCrn()));
    }

    public StackDto getByNameOrCrn(NameOrCrn nameOrCrn, String accountId) {
        StackView stackView = nameOrCrn.hasName() ?
                stackViewRepository.findByName(accountId, nameOrCrn.getName()).orElseThrow(NotFoundException.notFound("Stack by name", nameOrCrn.getName())) :
                stackViewRepository.findByCrn(nameOrCrn.getCrn()).orElseThrow(NotFoundException.notFound("Stack by crn", nameOrCrn.getCrn()));
        return getStackProxy(stackView, false).orElseThrow(NotFoundException.notFound("StackDto", nameOrCrn.getNameOrCrn()));
    }

    public StackDto getById(Long id) {
        return getById(id, false);
    }

    public StackDto getById(Long id, boolean fetchResources) {
        StackView stackView = stackViewRepository.findById(id).orElseThrow(NotFoundException.notFound("Stack", id));
        return getStackProxy(stackView, fetchResources).orElseThrow(NotFoundException.notFound("StackDto", id));
    }

    public Optional<StackDto> getByIdOpt(Long id) {
        Optional<StackViewDelegate> stackView = stackViewRepository.findById(id);
        if (stackView.isEmpty()) {
            return Optional.empty();
        }
        return getStackProxy(stackView.get(), false);
    }

    public StackDto getByCrn(String crn) {
        StackView stackView = stackViewRepository.findByCrn(crn).orElseThrow(NotFoundException.notFound("Stack by crn", crn));
        return getStackProxy(stackView, false).orElseThrow(NotFoundException.notFound("StackDto by crn", crn));
    }

    private Optional<StackDto> getStackProxy(StackView stackView, boolean fetchResources) {
        Map<String, InstanceGroupDto> groupListMap = new HashMap<>();
        List<InstanceMetadataView> imDto = instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stackView.getId());
        Map<Long, Map<InstanceGroupView, List<InstanceMetadataView>>> group = new HashMap<>();
        List<InstanceGroupView> instanceGroups = instanceGroupService.getInstanceGroupViewByStackId(stackView.getId());
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
                groupListMap.put(instanceGroupView.getGroupName(), new InstanceGroupDto(instanceGroupView, new ArrayList<>()));
            } else {
                InstanceGroupView instanceGroupView = map.keySet().stream().findFirst().get();
                List<InstanceMetadataView> instanceMetadataViews = map.values().stream().findFirst().orElse(new ArrayList<>());
                groupListMap.put(instanceGroupView.getGroupName(), new InstanceGroupDto(instanceGroupView, instanceMetadataViews));
            }
        });
        Set<Resource> resources = null;
        if (fetchResources) {
            resources = new HashSet<>(resourceService.getAllByStackIdAndStatuses(stackView.getId(), Set.of(CommonStatus.CREATED)));
        }
        ClusterView cluster = clusterViewRepository.findByStackId(stackView.getId()).orElse(null);
        Network network = stackViewRepository.getNetworkByStackById(stackView.getId()).orElse(null);
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
            List<ComponentType> types = List.of(ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CM_REPO_DETAILS);
            components = clusterComponentConfigProvider.getComponentsByClusterIdAndInComponentType(cluster.getId(), types);
        }
        List<StackParameters> parameters = stackParametersRepository.findAllByStackId(stackView.getId());
        SecurityConfig securityConfig = stackViewRepository.getSecurityByStackId(stackView.getId());
        Map<InstanceGroupView, List<String>> availabilityZonesByStackId = new HashMap<>();
        return Optional.of(new StackDto(stackView, cluster, network, workspace, workspace.getTenant(), groupListMap, resources, blueprint, gateway,
                orchestrator, fileSystem, additionalFileSystem, components, parameters, securityConfig, availabilityZonesByStackId));
    }

    public List<InstanceGroupDto> getInstanceMetadataByInstanceGroup(Long stackId) {
        List<InstanceGroupDto> groupListMap = new ArrayList<>();
        List<InstanceMetadataView> imDto = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stackId);
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
        return nameOrCrn.hasName() ? stackViewRepository.findByName(accountId, nameOrCrn.getName())
                .orElseThrow(NotFoundException.notFound("Stack by name", nameOrCrn.getName())) :
                stackViewRepository.findByCrn(nameOrCrn.getCrn())
                        .orElseThrow(NotFoundException.notFound("Stack by crn", nameOrCrn.getCrn()));
    }

    public Optional<StackView> getStackViewByNameOrCrnOpt(NameOrCrn nameOrCrn, String accountId) {
        Optional<StackViewDelegate> stackViewDelegate = nameOrCrn.hasName() ?
                stackViewRepository.findByName(accountId, nameOrCrn.getName()) :
                stackViewRepository.findByCrn(nameOrCrn.getCrn());
        return Optional.ofNullable(stackViewDelegate.orElse(null));
    }

    public StackView getStackViewByName(String name, String accountId) {
        return stackViewRepository.findByName(accountId, name).orElseThrow(NotFoundException.notFound("Stack by name", name));
    }

    public StackView getStackViewByCrn(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackViewRepository.findByCrn(crn);
        return stackViewDelegate.orElseThrow(NotFoundException.notFound("Stack by crn", crn));
    }

    public Optional<StackView> getStackViewByCrnOpt(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackViewRepository.findByCrn(crn);
        return Optional.ofNullable(stackViewDelegate.orElseThrow(null));
    }

    public StackView getStackViewById(Long id) {
        return stackViewRepository.findById(id).orElseThrow(NotFoundException.notFound("Stack", id));
    }

    public Optional<StackView> getStackViewByIdOpt(Long id) {
        Optional<StackViewDelegate> stackViewDelegate = stackViewRepository.findById(id);
        return Optional.ofNullable(stackViewDelegate.orElseThrow(null));
    }

    public ClusterView getClusterViewByStackId(Long id) {
        return clusterViewRepository.findByStackId(id).orElse(null);
    }

    public StackView getByName(String accountId, String name, StackType stackType,
            ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        return stackType == null ?
                stackViewRepository.findByName(accountId, name, config.isActive(),
                        config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", name)) :
                stackViewRepository.findByName(accountId, name, stackType, config.isActive(),
                        config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", name));
    }

    public StackView getByCrn(String crn, StackType stackType, ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        return stackType == null ?
                stackViewRepository.findByCrn(crn, config.isActive(), config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", crn)) :
                stackViewRepository.findByCrn(crn, stackType, config.isActive(), config.showAfterMillisecs())
                        .orElseThrow(NotFoundException.notFound("Stack", crn));
    }

    public List<StackParameters> getStackParameters(Long stackId) {
        return stackParametersRepository.findAllByStackId(stackId);
    }

    public List<StackView> findNotTerminatedByCrns(Collection<String> resourceCrns) {
        return new ArrayList<>(stackViewRepository.findAllByResourceCrnIn(resourceCrns));
    }

    public Optional<StackView> findNotTerminatedByCrn(String resourceCrn) {
        Optional<StackViewDelegate> stackView = stackViewRepository.findByResourceCrn(resourceCrn);
        return Optional.ofNullable(stackView.orElse(null));
    }

    public List<StackView> findNotTerminatedByNamesAndAccountId(List<String> resourceNames, String accountId) {
        return new ArrayList<>(stackViewRepository.findAllByNamesAndAccountId(resourceNames, accountId));
    }

    public Optional<StackView> findNotTerminatedByNameAndAccountId(String resourceName, String accountId) {
        Optional<StackViewDelegate> stackView = stackViewRepository.findByNameAndAccountId(resourceName, accountId);
        return Optional.ofNullable(stackView.orElse(null));
    }
}
