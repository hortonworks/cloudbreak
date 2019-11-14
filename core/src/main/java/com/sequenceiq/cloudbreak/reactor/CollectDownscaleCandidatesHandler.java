package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectDownscaleCandidatesHandler implements EventHandler<CollectDownscaleCandidatesRequest> {

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

    @Inject
    private TransactionService transactionService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectDownscaleCandidatesRequest.class);
    }

    @Override
    public void accept(Event<CollectDownscaleCandidatesRequest> event) {
        CollectDownscaleCandidatesRequest request = event.getData();
        CollectDownscaleCandidatesResult result;
        try {
            result = transactionService.required(() -> {
                try {
                    Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
                    int defaultRootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(stack.cloudPlatform());
                    Set<Long> privateIds = request.getPrivateIds();
                    if (noSelectedInstancesForDownscale(privateIds)) {
                        privateIds = collectCandidates(request, stack, defaultRootVolumeSize);
                    } else {
                        List<InstanceMetaData> removableInstances =
                                stackService.getInstanceMetaDataForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
                        List<InstanceMetaData> removableAndNotDeletedInstances = removableInstances.stream()
                                .filter(instanceMetaData -> !instanceMetaData.isTerminated() && !instanceMetaData.isDeletedOnProvider())
                                .collect(Collectors.toList());
                        if (!request.getDetails().isForced()) {
                            clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                                    .verifyNodesAreRemovable(stack, removableAndNotDeletedInstances);
                        }
                    }
                    return new CollectDownscaleCandidatesResult(request, privateIds);
                } catch (Exception e) {
                    return new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
                }
            });
        } catch (TransactionExecutionException e) {
            throw e.getCause();
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private Set<Long> collectCandidates(CollectDownscaleCandidatesRequest request, Stack stack, int defaultRootVolumeSize)
            throws CloudbreakException {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), request.getHostGroupName())
                .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));
        List<InstanceMetaData> metaDatasForInstanceGroup = instanceMetaDataService.findAliveInstancesInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> collectedCandidates = clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                .collectDownscaleCandidates(hostGroup, request.getScalingAdjustment(), defaultRootVolumeSize, new HashSet<>(metaDatasForInstanceGroup));
        String collectedHostsAsString = collectedCandidates.stream().map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null ?
                "FQDN: " + instanceMetaData.getDiscoveryFQDN() : "Private id: " + instanceMetaData.getPrivateId())
                .collect(Collectors.joining(","));

        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_SELECT_FOR_DOWNSCALE, AVAILABLE.name(), collectedHostsAsString);
        return collectedCandidates.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
    }

    private boolean noSelectedInstancesForDownscale(Set<Long> privateIds) {
        return privateIds == null || privateIds.isEmpty();
    }
}
