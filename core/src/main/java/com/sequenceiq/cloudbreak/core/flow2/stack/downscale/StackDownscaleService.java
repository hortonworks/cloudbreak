package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DOWNSCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DOWNSCALE_INSTANCES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DOWNSCALE_SUCCESS;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.util.StringUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.InstanceMetadataProcessor;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class StackDownscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private InstanceMetadataProcessor instanceMetadataProcessor;

    public void startStackDownscale(StackScalingFlowContext context, StackDownscaleTriggerEvent stackDownscaleTriggerEvent) {
        LOGGER.debug("Downscaling of stack {}", context.getStack().getId());
        stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        List<Long> privateIds = null;
        if (MapUtils.isNotEmpty(stackDownscaleTriggerEvent.getHostGroupsWithPrivateIds())) {
            privateIds = stackDownscaleTriggerEvent.getHostGroupsWithPrivateIds().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        } else if (MapUtils.isNotEmpty(context.getHostGroupWithPrivateIds())) {
            privateIds = context.getHostGroupWithPrivateIds().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }
        String msgParam;
        if (CollectionUtils.isNotEmpty(privateIds)) {
            List<String> instanceIdList = instanceMetaDataService
                    .getInstanceMetadataViewsByStackIdAndPrivateIds(context.getStackId(), privateIds).stream()
                    .map(InstanceMetadataView::getInstanceId)
                    .collect(toList());
            msgParam = String.join(",", instanceIdList);
        } else if (stackDownscaleTriggerEvent.getHostGroupsWithAdjustment() != null) {
            Integer adjustmentSize = stackDownscaleTriggerEvent.getHostGroupsWithAdjustment().values().stream().reduce(0, Integer::sum);
            msgParam = String.valueOf(adjustmentSize);
        } else {
            throw new CloudbreakRuntimeException("No adjustment was defined");
        }
        flowMessageService.fireEventAndLog(context.getStack().getId(), UPDATE_IN_PROGRESS.name(), STACK_DOWNSCALE_INSTANCES, msgParam);
    }

    public void finishStackDownscale(StackScalingFlowContext context, Collection<Long> privateIds)
            throws TransactionExecutionException {
        StackView stack = context.getStack();
        stackScalingService.updateInstancesToTerminated(privateIds, stack.getId());
        List<InstanceMetadataView> instanceMetaDatas = instanceMetaDataService.getInstanceMetadataViewsByStackIdAndPrivateIds(stack.getId(), privateIds);
        if (context.isRepair()) {
            fillDiscoveryFQDNForRepair(stack, instanceMetaDatas);
        }
        cleanupDnsRecords(stack, instanceMetaDatas);
        List<String> deletedInstanceIds = instanceMetaDatas.stream()
                .map(instanceMetaData ->
                        instanceMetaData.getInstanceId() != null ? instanceMetaData.getInstanceId() : instanceMetaData.getPrivateId().toString())
                .collect(Collectors.toList());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_COMPLETED,
                String.format("Downscale of the cluster infrastructure finished successfully. Terminated node(s): %s", deletedInstanceIds));
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_DOWNSCALE_SUCCESS, String.join(",", deletedInstanceIds));
    }

    private void fillDiscoveryFQDNForRepair(StackView stack, List<InstanceMetadataView> removableInstances) {
        List<Resource> diskResources = resourceService.findByStackIdAndType(stack.getId(), stack.getDiskResourceType());
        List<String> removeableInstanceIds = removableInstances
                .stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        for (Resource volumeSet : diskResources) {
            Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
            attributes.ifPresent(volumeSetAttributes ->
                    fillDiscoveryFQDNInVolumeSetIfEmpty(removableInstances, removeableInstanceIds, volumeSet, volumeSetAttributes));
        }
        resourceService.saveAll(diskResources);
    }

    private void fillDiscoveryFQDNInVolumeSetIfEmpty(List<InstanceMetadataView> removableInstances, List<String> removeableInstanceIds, Resource volumeSet,
        VolumeSetAttributes volumeSetAttributes) {
        if (removeableInstanceIds.contains(volumeSet.getInstanceId())
                && StringUtils.isNullOrEmpty(volumeSetAttributes.getDiscoveryFQDN())) {
            Optional<InstanceMetadataView> metaData = removableInstances.stream()
                    .filter(instanceMetaData -> volumeSet.getInstanceId().equals(instanceMetaData.getInstanceId()))
                    .findFirst();
            metaData.ifPresent(im -> {
                volumeSetAttributes.setDiscoveryFQDN(im.getDiscoveryFQDN());
                resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
            });
        }
    }

    private void cleanupDnsRecords(StackView stack, Collection<InstanceMetadataView> instanceMetaDatas) {
        Set<String> fqdns = instanceMetadataProcessor.extractFqdn(instanceMetaDatas);
        Set<String> ips = Set.of();
        try {
            LOGGER.info("Cleanup DNS records for FQDNS: {} and IPs {}", fqdns, ips);
            freeIpaCleanupService.cleanupDnsOnly(stack, fqdns, ips);
        } catch (Exception e) {
            LOGGER.error("Failed to delete dns records for FQDNS: {} and IPs {}", fqdns, ips, e);
        }
    }

    public void handleStackDownscaleError(StackFailureContext context, Exception errorDetails) {
        LOGGER.info("Exception during the downscaling of stack", errorDetails);
        flowMessageService.fireEventAndLog(context.getStackId(), UPDATE_FAILED.name(), STACK_DOWNSCALE_FAILED, errorDetails.getMessage());
        stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.DOWNSCALE_FAILED, errorDetails.getMessage());
    }
}
