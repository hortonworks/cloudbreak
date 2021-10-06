package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SELECT_FOR_DOWNSCALE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectDownscaleCandidatesHandler implements EventHandler<CollectDownscaleCandidatesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectDownscaleCandidatesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceService resourceService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectDownscaleCandidatesRequest.class);
    }

    @Override
    public void accept(Event<CollectDownscaleCandidatesRequest> event) {
        CollectDownscaleCandidatesRequest request = event.getData();
        CollectDownscaleCandidatesResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Collection<Resource> resources = resourceService.getAllByStackId(stack.getId());
            stack.setResources(new HashSet<>(resources));
            Set<Long> privateIds = request.getPrivateIds();
            if (CollectionUtils.isEmpty(privateIds)) {
                LOGGER.info("No private id(s) has been provided for downscale!");
                privateIds = collectCandidates(request, stack);
            } else {
                List<InstanceMetaData> removableInstances =
                        stackService.getInstanceMetaDataForPrivateIdsWithoutTerminatedInstances(stack.getInstanceMetaDataAsList(), privateIds);
                List<InstanceMetaData> removableAndNotDeletedInstances = removableInstances.stream()
                        .filter(instanceMetaData -> !instanceMetaData.isTerminated() && !instanceMetaData.isDeletedOnProvider())
                        .collect(Collectors.toList());
                LOGGER.info("The following instance(s) can be removed: [{}]", removableInstances
                        .stream()
                        .map(imd -> "InstanceID: " + imd.getInstanceId())
                        .collect(Collectors.joining(", ")));
                if (!request.getDetails().isForced()) {
                    clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                            .verifyNodesAreRemovable(stack, removableAndNotDeletedInstances);
                }
            }
            LOGGER.info("Moving ahead with " + CollectDownscaleCandidatesResult.class.getSimpleName() + " with the following request [{}] " +
                            "and private IDs: [{}]", request,
                    privateIds.stream().map(id -> String.valueOf(id)).collect(Collectors.joining(", ")));
            if (isEmpty(privateIds)) {
                LOGGER.info("No instances met downscale criteria in host group: {}", request.getHostGroupName());
            } else {
                LOGGER.info("Selected {} instances based on downscale request {}.", privateIds, request);
            }
            result = new CollectDownscaleCandidatesResult(request, privateIds);
        } catch (Exception e) {
            LOGGER.warn("Something has happened while CB wanted to collect candidates for downscale!", e);
            result = new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private Set<Long> collectCandidates(CollectDownscaleCandidatesRequest request, Stack stack)
            throws CloudbreakException {
        LOGGER.debug("Collecting candidates for downscale based on [{}] and stack CRN [{}].", request, stack.getResourceCrn());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), request.getHostGroupName())
                .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));
        LOGGER.debug("Host group has been found for cluster! It's name: {}", hostGroup.getName());
        List<InstanceMetaData> metaDataForInstanceGroup = instanceMetaDataService.findAliveInstancesInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> collectedCandidates = clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                .collectDownscaleCandidates(hostGroup, request.getScalingAdjustment(), new HashSet<>(metaDataForInstanceGroup));
        String collectedHostsAsString = collectedCandidates.stream().map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null ?
                "FQDN: " + instanceMetaData.getDiscoveryFQDN() : "Private id: " + instanceMetaData.getPrivateId())
                .collect(Collectors.joining(", "));
        LOGGER.debug("The following hosts has been collected as candidates for downscale: [{}]", collectedHostsAsString);
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_SELECT_FOR_DOWNSCALE, collectedHostsAsString);
        return collectedCandidates.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
    }

}
