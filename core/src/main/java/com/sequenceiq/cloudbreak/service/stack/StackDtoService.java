package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.StackDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayDto;
import com.sequenceiq.cloudbreak.repository.ClusterDtoRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.CommonStatus;

@Component
public class StackDtoService {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackDtoRepository stackDtoRepository;

    @Inject
    private ClusterDtoRepository clusterDtoRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

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

    public StackProxy getByNameOrCrn(Long workspaceId, NameOrCrn nameOrCrn, StackType stackType, ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        var start = System.currentTimeMillis();
        StackDto stackDto = nameOrCrn.hasName() ?
                getByName(workspaceId, nameOrCrn.getName(), stackType, config):
                getByCrn(workspaceId, nameOrCrn.getCrn(), stackType, config);
        var d2 = System.currentTimeMillis() - start;
        return getStackProxy(stackDto, workspaceId);
    }

    public StackProxy getById(Long id) {
        StackDto stackDto = stackDtoRepository.findById(id).orElseThrow(NotFoundException.notFound("Stack", id));
        return getStackProxy(stackDto, stackDto.getWorkspaceId());
    }

    private StackProxy getStackProxy(StackDto stackDto, Long workspaceId) {
        var start = System.currentTimeMillis();
        var s = start;
//        List<InstanceMetaDataView> instanceMetadatas = instanceMetaDataRepository.findNotTerminatedAsOrderedListForStackWithoutEntityGraph(stackDto.getId());
//        Map<InstanceGroupView, List<InstanceMetaDataView>> groupListMap = instanceMetadatas.stream().collect(Collectors.groupingBy(InstanceMetaDataView::getInstanceGroup));
//        Map<InstanceGroupView, List<InstanceMetaDataView>> groupListMap = new HashMap<>();
        Map<InstanceGroupDto, List<InstanceMetadataDto>> groupListMap1 = new HashMap<>();
        var d2 = System.currentTimeMillis() - start;
        start = System.currentTimeMillis();
//        Map<InstanceGroupDto, List<InstanceMetadataDto>> gList = imDto.stream().collect(Collectors.groupingBy(InstanceMetadataDto::getInstanceGroup));
        var d4 = System.currentTimeMillis() - start;
        start = System.currentTimeMillis();
        var d5 = System.currentTimeMillis() - start;
        start = System.currentTimeMillis();
        List<InstanceMetadataDto> imDto = instanceMetaDataRepository.findNotTerminatedInstanaceMetadataDtoByStackId(stackDto.getId());
        Map<Long, Map<InstanceGroupDto, List<InstanceMetadataDto>>> group = new HashMap<>();
        List<InstanceGroupDto> imDto1 = instanceMetaDataRepository.findInstanceGroupDtoByStackId(stackDto.getId());
        imDto1.forEach(it -> group.put(it.getId(), new HashMap<>()));
        imDto.forEach(im -> {
            var imByIg = group.computeIfAbsent(im.getInstanceGroupId(), key -> new HashMap<>());
            if (imByIg.isEmpty()) {
                imByIg.put(imDto1.stream().filter(ig -> ig.getId().equals(im.getInstanceGroupId())).findFirst().get(), new ArrayList<>());
            }
            imByIg.values().stream().findFirst().get().add(im);
        });
        group.forEach((id, map) -> {
            if (map.isEmpty()) {
                groupListMap1.put(imDto1.stream().filter(ig -> ig.getId().equals(id)).findFirst().get(), new ArrayList<>());
            } else {
                groupListMap1.put(map.keySet().stream().findFirst().get(), map.values().stream().findFirst().orElse(new ArrayList<>()));
            }
        });
        var d6 = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        Set<Resource> resources = new HashSet<>(resourceService.getAllByStackIdAndStatuses(stackDto.getId(), Set.of(CommonStatus.CREATED)));
        var d7 = System.currentTimeMillis() - start;
        ClusterDto cluster = clusterDtoRepository.findByStackId(stackDto.getId()).orElseThrow();
        Network network = stackDtoRepository.getNetworkByStackById(stackDto.getId()).orElseThrow();
        Workspace workspace = workspaceService.getByIdWithoutAuth(workspaceId);
        Blueprint blueprint = blueprintService.getByClusterId(cluster.getId()).orElseThrow();
        GatewayDto gateway = gatewayService.getByClusterId(cluster.getId()).orElseThrow();
        Orchestrator orchestrator = orchestratorService.getByStackId(stackDto.getId()).orElseThrow();
        FileSystem fileSystem = cluster.getFileSystem();
        FileSystem additionalFileSystem = cluster.getAdditionalFileSystem();
        Set<ClusterComponent> components = clusterComponentConfigProvider.getComponentsByClusterId(cluster.getId());
        var d8 = System.currentTimeMillis() - start;
        var duration = System.currentTimeMillis() - s;
        return new StackProxy(stackDto, cluster, network, workspace, groupListMap1, resources, blueprint, gateway, orchestrator, fileSystem, additionalFileSystem, components);
    }

    private StackDto getByName(Long workspaceId, String name, StackType stackType, ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        return stackType == null ?
                stackDtoRepository.findByName(workspaceId, name, config.isActive(), config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", name)):
                stackDtoRepository.findByName(workspaceId, name, stackType, config.isActive(), config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", name));
    }

    private StackDto getByCrn(Long workspaceId, String crn, StackType stackType, ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig config) {
        return stackType == null ?
                stackDtoRepository.findByCrn(workspaceId, crn, config.isActive(), config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", crn)):
                stackDtoRepository.findByCrn(workspaceId, crn, stackType, config.isActive(), config.showAfterMillisecs()).orElseThrow(NotFoundException.notFound("Stack", crn));
    }
}
