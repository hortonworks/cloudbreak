package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SELECT_FOR_DOWNSCALE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CollectDownscaleCandidatesHandler implements EventHandler<CollectDownscaleCandidatesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectDownscaleCandidatesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CollectDownscaleCandidatesRequest.class);
    }

    @Override
    public void accept(Event<CollectDownscaleCandidatesRequest> event) {
        CollectDownscaleCandidatesRequest request = event.getData();
        CollectDownscaleCandidatesResult result;
        try {
            StackDto stack = stackDtoService.getById(request.getResourceId());
            Set<Long> privateIds = request.getHostGroupWithPrivateIds().values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(privateIds)) {
                LOGGER.info("No private id(s) has been provided for downscale!");
                privateIds = collectCandidates(request, stack);
            } else {
                List<InstanceMetadataView> removableInstances = stack.getInstanceMetaDataForPrivateIds(privateIds);
                List<InstanceMetadataView> removableAndNotDeletedInstances = removableInstances.stream()
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
                    privateIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            if (isEmpty(privateIds)) {
                LOGGER.info("No instances met downscale criteria in host groups: {}",
                        String.join(", ", request.getHostGroupWithAdjustment().keySet()));
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

    private Set<Long> collectCandidates(CollectDownscaleCandidatesRequest request, StackDto stack)
            throws CloudbreakException {
        Set<Long> privateIds = new HashSet<>();
        LOGGER.debug("Collecting candidates for downscale based on [{}] and stack CRN [{}].", request, stack.getResourceCrn());
        for (Map.Entry<String, Integer> entry : request.getHostGroupWithAdjustment().entrySet()) {
            String hostGroupName = entry.getKey();
            LOGGER.debug("Host group has been found for cluster! Its name: {}", hostGroupName);
            List<InstanceMetadataView> metaDataForInstanceGroup = stack.getInstanceGroupByInstanceGroupName(hostGroupName).getInstanceMetadataViews();
            Set<InstanceMetadataView> collectedCandidates = clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                    .collectDownscaleCandidates(hostGroupName, entry.getValue(), new HashSet<>(metaDataForInstanceGroup));
            String collectedHostsAsString = collectedCandidates.stream().map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null ?
                    "FQDN: " + instanceMetaData.getDiscoveryFQDN() : "Private id: " + instanceMetaData.getPrivateId())
                    .collect(Collectors.joining(", "));
            LOGGER.debug("The following hosts has been collected as candidates for downscale: [{}]", collectedHostsAsString);
            flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_SELECT_FOR_DOWNSCALE, collectedHostsAsString);
            privateIds.addAll(collectedCandidates.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet()));
        }
        return privateIds;
    }

}
