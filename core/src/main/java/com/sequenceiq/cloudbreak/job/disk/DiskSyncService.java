package com.sequenceiq.cloudbreak.job.disk;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_FAILED;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskSyncMode;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskInstanceInfoCollector;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.ProviderSyncState;

@Service
public class DiskSyncService {

    public static final Map<String, ResourceType> CLOUD_RESOURCE_TYPE_CONSTANTS = Map.of(
            AZURE.name(), AZURE_VOLUMESET,
            AWS.name(), AWS_VOLUMESET
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSyncService.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceSyncUtil resourceSyncUtil;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private StackStatusAndReachabilityValidatorUtil stackStatusAndReachabilityValidatorUtil;

    @Inject
    private DiskInstanceInfoCollector diskInstanceInfoCollector;

    @Inject
    private StackUpdater stackUpdater;

    public void syncResources(StackDto stackDto, DiskSyncMode diskSyncMode) {
        Stack stack = stackService.getByIdWithLists(stackDto.getId());
        DetailedStackStatus stackStatus = stack.getDetailedStatus();
        try {
            if (!stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)) {
                throw new CloudbreakServiceException("The stack is either not in a valid state or not all nodes or reachable for disk sync to run!");
            }
            boolean alreadyReported = stackDto.getStack().getProviderSyncStates().contains(ProviderSyncState.DISK_MISMATCH_FOUND);
            List<Resource> volumeSetResources = resourceService.findAllByStackIdAndResourceTypeIn(stackDto.getId(),
                    List.of(CLOUD_RESOURCE_TYPE_CONSTANTS.get(stackDto.getCloudPlatform())));
            Map<String, List<VolumeRecord>> cloudMetadata = diskInstanceInfoCollector.getCloudMetadataMap(stackDto);
            Map<String, String> fqdnInstanceIdMap = getFqdnInstanceIdMap(stackDto);
            Map<String, InstanceResourceDto> saltInfoMap = diskInstanceInfoCollector.getAndParseSaltInfo(stackDto, fqdnInstanceIdMap, cloudMetadata,
                    stackDto.getCloudPlatform());
            // suppress the customer-facing warning events once the mismatch has already been reported (alreadyReported), while still detecting it
            boolean unmounted = resourceSyncUtil.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack, alreadyReported);
            boolean mismatch = resourceSyncUtil.updateResource(volumeSetResources, saltInfoMap, stackDto, diskSyncMode, alreadyReported);
            boolean mismatchFound = unmounted || mismatch;
            updateProviderSyncState(stackDto, diskSyncMode, mismatchFound, alreadyReported);
        } catch (Exception ex) {
            LOGGER.error("Exception while running disk sync job on stack {}. Exception::", stackDto.getId(), ex);
            eventService.fireCloudbreakEvent(stackDto.getId(), stackStatus.name(), DISK_SYNC_FAILED, Collections.singletonList(ex.getMessage()));
            throw new CloudbreakServiceException("Exception while trying to sync disks - " + ex.getMessage());
        }
    }

    private void updateProviderSyncState(StackDto stackDto, DiskSyncMode diskSyncMode, boolean mismatchFound, boolean alreadyReported) {
        if (DiskSyncMode.PERSIST == diskSyncMode) {
            // remediation flow ran -> immediate clear; if anything genuinely remains, the next DRY_RUN re-fires once
            LOGGER.info("Disk sync remediation (PERSIST) completed for stack {}, clearing {} state so future mismatches warn again",
                    stackDto.getId(), ProviderSyncState.DISK_MISMATCH_FOUND);
            stackUpdater.removeProviderStates(stackDto.getResourceCrn(), stackDto.getId(), Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));
        } else if (mismatchFound && !alreadyReported) {
            // transition into mismatch: warning already fired once above, now record the condition to suppress further warnings
            LOGGER.info("Disk mismatch detected for stack {} and not previously reported; marking {} to suppress repeated warning events",
                    stackDto.getId(), ProviderSyncState.DISK_MISMATCH_FOUND);
            stackUpdater.addProviderState(stackDto.getResourceCrn(), stackDto.getId(), ProviderSyncState.DISK_MISMATCH_FOUND);
        } else if (!mismatchFound && alreadyReported) {
            // transition back to clean: self-heal so a genuinely new mismatch warns again
            LOGGER.info("Disk mismatch resolved for stack {}; clearing {} so a genuinely new mismatch warns again",
                    stackDto.getId(), ProviderSyncState.DISK_MISMATCH_FOUND);
            stackUpdater.removeProviderStates(stackDto.getResourceCrn(), stackDto.getId(), Set.of(ProviderSyncState.DISK_MISMATCH_FOUND));
        } else {
            LOGGER.debug("No disk-mismatch state change for stack {} (mismatchFound={}, alreadyReported={}, mode={})",
                    stackDto.getId(), mismatchFound, alreadyReported, diskSyncMode);
        }
    }

    private Map<String, String> getFqdnInstanceIdMap(StackDto stack) {
        return stack.getInstanceGroupDtos().stream()
                .flatMap(ig -> ig.getNotDeletedInstanceMetaData().stream())
                .collect(toMap(InstanceMetadataView::getDiscoveryFQDN, InstanceMetadataView::getInstanceId));
    }
}
