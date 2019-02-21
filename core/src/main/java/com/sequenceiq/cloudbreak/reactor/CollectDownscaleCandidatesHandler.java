package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectDownscaleCandidatesHandler implements ReactorEventHandler<CollectDownscaleCandidatesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectDownscaleCandidatesRequest.class);
    }

    @Override
    public void accept(Event<CollectDownscaleCandidatesRequest> event) {
        CollectDownscaleCandidatesRequest request = event.getData();
        CollectDownscaleCandidatesResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            int defaultRootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(stack.cloudPlatform());
            Set<Long> privateIds = request.getPrivateIds();
            if (noSelectedInstancesForDownscale(privateIds)) {
                privateIds = collectCandidates(request, stack, defaultRootVolumeSize);
            } else {
                List<InstanceMetaData> instanceMetaDataList = stackService.getInstanceMetaDataForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
                List<InstanceMetaData> notDeletedNodes = instanceMetaDataList.stream()
                        .filter(instanceMetaData -> !instanceMetaData.isTerminated() && !instanceMetaData.isDeletedOnProvider())
                        .collect(Collectors.toList());
                if (!request.getDetails().isForced()) {
                    Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
                    Multimap<Long, HostMetadata> hostGroupWithInstances = getHostGroupWithInstances(stack, notDeletedNodes);
                    clusterApiConnectors.getConnector(stack).clusterDecomissionService().verifyNodesAreRemovable(hostGroupWithInstances, hostGroups,
                            defaultRootVolumeSize, notDeletedNodes);
                }
            }
            result = new CollectDownscaleCandidatesResult(request, privateIds);
        } catch (Exception e) {
            result = new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private Multimap<Long, HostMetadata> getHostGroupWithInstances(Stack stack, List<InstanceMetaData> instanceMetaDataList) {
        List<InstanceMetaData> instancesWithHostName = instanceMetaDataList.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .collect(Collectors.toList());

        Multimap<Long, HostMetadata> hostGroupWithInstances = ArrayListMultimap.create();
        for (InstanceMetaData instanceMetaData : instancesWithHostName) {
            HostMetadata hostMetadata = hostGroupService.getHostMetadataByClusterAndHostName(stack.getCluster(), instanceMetaData.getDiscoveryFQDN());
            if (hostMetadata != null) {
                hostGroupWithInstances.put(hostMetadata.getHostGroup().getId(), hostMetadata);
            }
        }
        return hostGroupWithInstances;
    }

    private Set<Long> collectCandidates(CollectDownscaleCandidatesRequest request, Stack stack, int defaultRootVolumeSize)
            throws CloudbreakException {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), request.getHostGroupName());
        Set<InstanceMetaData> instanceMetaDatasInStack = instanceMetaDataService.findAllInStack(stack.getId());
        Set<String> hostNames = clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                .collectDownscaleCandidates(hostGroup, request.getScalingAdjustment(), defaultRootVolumeSize, instanceMetaDatasInStack);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_SELECT_FOR_DOWNSCALE, AVAILABLE.name(), hostNames);
        return stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), hostNames);
    }

    private boolean noSelectedInstancesForDownscale(Set<Long> privateIds) {
        return privateIds == null || privateIds.isEmpty();
    }
}
