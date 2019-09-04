package com.sequenceiq.cloudbreak.reactor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;
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
    private AmbariDecommissioner ambariDecommissioner;

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
            Set<Long> privateIds = request.getPrivateIds();
            if (noSelectedInstancesForDownscale(privateIds)) {
                privateIds = collectCandidates(request, stack);
            } else {
                List<InstanceMetaData> instanceMetaDataList = stackService.getInstanceMetaDataForPrivateIds(stack.getInstanceMetaDataAsList(), privateIds);
                List<InstanceMetaData> notDeletedNodes = instanceMetaDataList.stream()
                        .filter(instanceMetaData -> !instanceMetaData.isTerminated() && !instanceMetaData.isDeletedOnProvider())
                        .collect(Collectors.toList());
                if (!request.getDetails().isForced()) {
                    ambariDecommissioner.verifyNodesAreRemovable(stack, notDeletedNodes);
                }
            }
            result = new CollectDownscaleCandidatesResult(request, privateIds);
        } catch (Exception e) {
            result = new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private Set<Long> collectCandidates(CollectDownscaleCandidatesRequest request, Stack stack) throws CloudbreakException {
        Set<String> hostNames = ambariDecommissioner.collectDownscaleCandidates(stack, request.getHostGroupName(), request.getScalingAdjustment(),
                request.getDetails().isForced());
        return stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), hostNames);
    }

    private boolean noSelectedInstancesForDownscale(Set<Long> privateIds) {
        return privateIds == null || privateIds.isEmpty();
    }
}
