package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SELECT_FOR_DOWNSCALE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.util.StringUtils;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
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
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
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
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

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
            result = transactionService.required(() -> {
                try {
                    Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
                    int defaultRootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(stack.cloudPlatform());
                    Set<Long> privateIds = request.getPrivateIds();
                    if (noSelectedInstancesForDownscale(privateIds)) {
                        LOGGER.info("No private id(s) has been provided for downscale!");
                        privateIds = collectCandidates(request, stack, defaultRootVolumeSize);
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
                        fillDiscoveryFQDNForRepair(request, stack, removableInstances, removableAndNotDeletedInstances);
                    }
                    LOGGER.info("Moving ahead with " + CollectDownscaleCandidatesResult.class.getSimpleName() + " with the following request [{}] " +
                            "and private IDs: [{}]", request.toString(),
                            privateIds.stream().map(id -> String.valueOf(id)).collect(Collectors.joining(", ")));
                    if (isEmpty(privateIds)) {
                        String msgFmt = "Unable to collect instances [stackId: {}, hostGroup: {}] to downscale since none of them has meet the criteria " +
                                "for downscaling!";
                        LOGGER.info(msgFmt, stack.getId(), request.getHostGroupName());
                    }
                    return new CollectDownscaleCandidatesResult(request, privateIds);
                } catch (Exception e) {
                    LOGGER.warn("Something has happened while CB wanted to collect candidates for downscale!", e);
                    return new CollectDownscaleCandidatesResult(e.getMessage(), e, request);
                }
            });
        } catch (TransactionExecutionException e) {
            throw e.getCause();
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private void fillDiscoveryFQDNForRepair(CollectDownscaleCandidatesRequest request, Stack stack, List<InstanceMetaData> removableInstances,
            List<InstanceMetaData> removableAndNotDeletedInstances) {
        if (request.getDetails().isRepair()) {
            List<Resource> diskResources = resourceService.findByStackIdAndType(stack.getId(), stack.getDiskResourceType());
            List<String> removeableInstanceIds = removableAndNotDeletedInstances
                    .stream().map(InstanceMetaData::getInstanceId).collect(Collectors.toList());
            for (Resource volumeSet : diskResources) {
                Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
                attributes.ifPresent(volumeSetAttributes ->
                        fillDiscoveryFQDNInVolumeSetIfEmpty(removableInstances, removeableInstanceIds, volumeSet, volumeSetAttributes));
            }
            resourceService.saveAll(diskResources);
        }
    }

    private void fillDiscoveryFQDNInVolumeSetIfEmpty(List<InstanceMetaData> removableInstances, List<String> removeableInstanceIds, Resource volumeSet,
            VolumeSetAttributes volumeSetAttributes) {
        if (removeableInstanceIds.contains(volumeSet.getInstanceId())
                && StringUtils.isNullOrEmpty(volumeSetAttributes.getDiscoveryFQDN())) {
            Optional<InstanceMetaData> metaData = removableInstances.stream()
                    .filter(instanceMetaData -> volumeSet.getInstanceId().equals(instanceMetaData.getInstanceId()))
                    .findFirst();
            metaData.ifPresent(im -> {
                volumeSetAttributes.setDiscoveryFQDN(im.getDiscoveryFQDN());
                resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
            });
        }
    }

    private Set<Long> collectCandidates(CollectDownscaleCandidatesRequest request, Stack stack, int defaultRootVolumeSize)
            throws CloudbreakException {
        LOGGER.debug("Collecting candidates for downscale based on " + CollectDownscaleCandidatesRequest.class.getSimpleName() + " [{}] and "
                + Stack.class.getSimpleName() + " [{}] and default root volume size [{}]", request.toString(), "stackCrn: " + stack.getResourceCrn(),
                defaultRootVolumeSize);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), request.getHostGroupName())
                .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));
        LOGGER.debug("Host group has been found for cluster! It's name: {}", hostGroup.getName());
        List<InstanceMetaData> metaDataForInstanceGroup = instanceMetaDataService.findAliveInstancesInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> collectedCandidates = clusterApiConnectors.getConnector(stack).clusterDecomissionService()
                .collectDownscaleCandidates(hostGroup, request.getScalingAdjustment(), defaultRootVolumeSize, new HashSet<>(metaDataForInstanceGroup));
        String collectedHostsAsString = collectedCandidates.stream().map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null ?
                "FQDN: " + instanceMetaData.getDiscoveryFQDN() : "Private id: " + instanceMetaData.getPrivateId())
                .collect(Collectors.joining(", "));
        LOGGER.debug("The following hosts has been collected as candidates for downscale: [{}]", collectedHostsAsString);
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_SELECT_FOR_DOWNSCALE, collectedHostsAsString);
        return collectedCandidates.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
    }

    private boolean noSelectedInstancesForDownscale(Set<Long> privateIds) {
        return privateIds == null || privateIds.isEmpty();
    }

}
