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

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskInstanceInfoCollector;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.ResourceType;

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

    public void syncResources(StackDto stackDto, DiskSyncMode diskSyncMode) {
        Stack stack = stackService.getByIdWithLists(stackDto.getId());
        DetailedStackStatus stackStatus = stack.getDetailedStatus();
        try {
            if (!stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)) {
                throw new CloudbreakServiceException("The stack is either not in a valid state or not all nodes or reachable for disk sync to run!");
            }
            List<Resource> volumeSetResources = resourceService.findAllByStackIdAndResourceTypeIn(stackDto.getId(),
                    List.of(CLOUD_RESOURCE_TYPE_CONSTANTS.get(stackDto.getCloudPlatform())));
            Map<String, List<VolumeRecord>> cloudMetadata = diskInstanceInfoCollector.getCloudMetadataMap(stackDto);
            Map<String, String> fqdnInstanceIdMap = getFqdnInstanceIdMap(stackDto);
            Map<String, InstanceResourceDto> saltInfoMap = diskInstanceInfoCollector.getAndParseSaltInfo(stackDto, fqdnInstanceIdMap, cloudMetadata,
                    stackDto.getCloudPlatform());
            resourceSyncUtil.checkForUnmountedVolumes(saltInfoMap, fqdnInstanceIdMap, cloudMetadata, stack);
            resourceSyncUtil.updateResource(volumeSetResources, saltInfoMap, stackDto, diskSyncMode);
        } catch (Exception ex) {
            LOGGER.error("Exception while running disk sync job on stack {}. Exception::", stackDto.getId(), ex);
            eventService.fireCloudbreakEvent(stackDto.getId(), stackStatus.name(), DISK_SYNC_FAILED, Collections.singletonList(ex.getMessage()));
        }
    }

    private Map<String, String> getFqdnInstanceIdMap(StackDto stack) {
        return stack.getInstanceGroupDtos().stream()
                .flatMap(ig -> ig.getNotDeletedInstanceMetaData().stream())
                .collect(toMap(InstanceMetadataView::getDiscoveryFQDN, InstanceMetadataView::getInstanceId));
    }
}
