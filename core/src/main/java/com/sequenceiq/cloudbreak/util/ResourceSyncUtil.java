package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.DATABASE;
import static com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.GENERAL;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_FSTAB_MISMATCH_FOUND;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_VOLUME_MISMATCH_FOUND;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND;
import static com.sequenceiq.cloudbreak.job.disk.DiskSyncService.CLOUD_RESOURCE_TYPE_CONSTANTS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltService;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class ResourceSyncUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceSyncUtil.class);

    private static final String WHITESPACE_REGEX = "\\s+";

    private static final String NEWLINE_REGEX = "\n";

    private static final String EMPTY_STRING = "";

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    @Inject
    private StackService stackService;

    @Inject
    private SaltService saltService;

    @Inject
    private ResourceService resourceService;

    public boolean updateResource(Resource res, Map<String, InstanceResourceDto> saltInfoMap, StackDto stack) {
        String instanceId = res.getInstanceId();
        VolumeSetAttributes volumeSetAttribute = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class).orElseThrow();
        InstanceResourceDto instanceInfo = null;
        if (!saltInfoMap.isEmpty()) {
            instanceInfo = saltInfoMap.get(instanceId);
        }
        if (instanceInfo != null) {
            List<VolumeSetAttributes.Volume> syncedVolumes = syncResourceDisks(stack, volumeSetAttribute.getVolumes(),
                instanceInfo.getVolumes(), instanceId, res.getId());
            LOGGER.info("Synced volumes for resource id {}, instance id {}, volumes - {}", res.getId(), instanceId, syncedVolumes);
            volumeSetAttribute.setVolumes(syncedVolumes);

            // SYNC Fstab
            String fstabFromLsblk = createFstabFromLsblk(instanceInfo);
            LOGGER.info("DiskSyncJob: Created fstab from lsblk on instance: {}, fstab: {}", instanceId, fstabFromLsblk);
            instanceInfo.setFstab(fstabFromLsblk);
            syncFstab(instanceInfo, volumeSetAttribute, stack.getId(), res.getId(), instanceId, stack.getStatus().name());
        }
        // Add resourceService.save here if needed
        return true;
    }

    public String createFstabFromLsblk(InstanceResourceDto lsblkInfo) {
        return lsblkInfo.getVolumes().stream()
            .filter(dto -> dto.uuid() != null && !dto.uuid().isEmpty())
            .filter(dto -> dto.mountPoint() != null && !dto.mountPoint().isEmpty())
            .filter(dto -> !"/".equals(dto.mountPoint()) && !"/boot".equals(dto.mountPoint()) && !"/boot/efi".equals(dto.mountPoint()))
            .map(dto -> String.format("UUID=%s %s %s defaults,noatime,nofail 0 2", dto.uuid(), dto.mountPoint(), dto.fsType()))
            .collect(Collectors.joining("\n"));
    }

    public void syncFstab(InstanceResourceDto instanceInfo, VolumeSetAttributes volumeSetAttributeFromDB,
        Long stackId, Long resourceId, String instanceId, String stackStatus) {
        String normalizedFstabFromDB = normalizeFstab(volumeSetAttributeFromDB.getFstab());
        if (getMountedVolumesCount(instanceInfo.getFstab()) != getMountedVolumesCount(normalizedFstabFromDB)) {
            LOGGER.info("Found fstab mismatch in Resource table: saved fstab: {} and actual fstab created from lsblk: {}",
                normalizedFstabFromDB, instanceInfo.getFstab());
            eventService.fireCloudbreakEvent(stackId, stackStatus, DISK_SYNC_FSTAB_MISMATCH_FOUND,
                    Arrays.asList(instanceId, normalizedFstabFromDB, instanceInfo.getFstab(), String.valueOf(resourceId)));
        }
    }

    public String normalizeFstab(String fstab) {
        if (fstab == null) {
            return EMPTY_STRING;
        }
        return fstab.lines()
            .map(String::trim)
            .filter(l -> !l.isEmpty())
            .map(l -> l.replaceAll(WHITESPACE_REGEX, " "))
            // Remove duplicate lines
            .distinct()
            // Join them back with newlines (cleaner than reduce)
            .collect(Collectors.joining(NEWLINE_REGEX));
    }

    @VisibleForTesting
    List<VolumeSetAttributes.Volume> syncResourceDisks(StackDto stack, List<VolumeSetAttributes.Volume> databaseList,
        List<InstanceResourceDto.VolumeDto> saltDisks, String instanceId, Long resourceId) {
        List<VolumeSetAttributes.Volume> existingDisksOnInstance = saltDisks.stream()
            .map(saltDisk -> new VolumeSetAttributes.Volume(
                saltDisk.volumeId(),
                saltDisk.deviceName(),
                saltDisk.size(),
                saltDisk.volumeType(),
                saltDisk.mountPoint() != null && saltDisk.mountPoint().contains("dbfs") ? DATABASE : GENERAL)
            )
            .toList();
        if (databaseList != null && existingDisksOnInstance.size() != databaseList.size()) {
            LOGGER.info("Found volume mismatch in Resource table: saved resources : {} and actual resources are : {}", databaseList, existingDisksOnInstance);
            eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), DISK_SYNC_VOLUME_MISMATCH_FOUND,
                Arrays.asList(instanceId, databaseList.toString(), existingDisksOnInstance.toString(), String.valueOf(resourceId)));
        }
        return existingDisksOnInstance;
    }

    public Map<String, Long> countHadoopMountsPerServer(Map<String, InstanceResourceDto> saltInfoMap) {
        if (saltInfoMap == null) {
            return Collections.emptyMap();
        }

        return saltInfoMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<InstanceResourceDto.VolumeDto> volumes = entry.getValue().getVolumes();
                    return volumes != null ? (long) volumes.size() : 0L;
                }
            ));
    }

    public long getMountedVolumesCount(String fstabContent) {
        if (fstabContent == null || fstabContent.isEmpty()) {
            return 0L;
        }

        return Arrays.stream(fstabContent.split("\\r?\\n"))
            .map(String::trim)
            // 1. Filter out comments and empty lines
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .map(line -> line.split(WHITESPACE_REGEX))
            // 2. Ensure line has at least Device and MountPoint columns
            .filter(columns -> columns.length >= 2)
            // 3. Match the mount point column (index 1)
            .filter(columns -> columns[1].startsWith("/hadoopfs/fs") || columns[1].startsWith("/dbfs") || columns[1].startsWith("/hadoopfs/ephfs"))
            .count();
    }

    public Map<String, String> getFstabInformation(Long stackId) {
        Stack stack = stackService.getByIdWithLists(stackId);
        List<Resource> volumeSetResources = resourceService.findAllByStackIdAndResourceTypeIn(stackId,
            List.of(CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())));
        stack.setResources(new HashSet<>(volumeSetResources));
        Set<Node> nodesWithDiskData = new HashSet<>(stackUtil.collectNodesWithDiskData(stack));
        GatewayConfig primaryGateway = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Target<String> allHosts = new HostList(nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try {
            LOGGER.info("Fetching fstab from instances to validate against CB context - Nodes - " + allHosts);
            Map<String, Map<String, String>> saltFstabInfo =  saltOrchestrator.getFstabInformation(saltService.createSaltConnector(primaryGateway),
                allHosts, nodesWithDiskData);
            return saltFstabInfo.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, entry -> entry.getValue().getOrDefault("fstab", EMPTY_STRING)));
        } catch (Exception e) {
            LOGGER.warn("Exception while fetching fstab information for nodes - " + allHosts + " :: Exception - " + e);
            throw e;
        }
    }

    public void checkForUnmountedVolumes(Map<String, InstanceResourceDto> saltInfoMap, Map<String, String> fqdnInstanceIdMap,
        Map<String, List<VolumeRecord>> cloudMetadata, Stack stack) {
        Map<String, Long> saltLsblkDiskCountsPerInstanceIdMap = countHadoopMountsPerServer(saltInfoMap);
        Map<String, Integer> cloudProviderFqdnVolumeCountMap = fqdnInstanceIdMap.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                entry -> {
                    String instanceId = entry.getValue();
                    return cloudMetadata.getOrDefault(instanceId, Collections.emptyList()).size();
                }
            ));
        Map<String, List<String>> groupToFqdnMap = stack.getInstanceGroupDtos().stream().flatMap(ig -> ig.getNotDeletedInstanceMetaData().stream())
            .collect(groupingBy(
                InstanceMetadataView::getInstanceGroupName,
                mapping(InstanceMetadataView::getDiscoveryFQDN, toList())
            ));
        for (Map.Entry<String, List<String>> groupEntry : groupToFqdnMap.entrySet()) {
            String group = groupEntry.getKey();

            for (String fqdn : groupEntry.getValue()) {
                String instanceId = fqdnInstanceIdMap.getOrDefault(fqdn, "");
                int saltCount = saltLsblkDiskCountsPerInstanceIdMap.getOrDefault(instanceId, 0L).intValue();
                int cloudCount = cloudProviderFqdnVolumeCountMap.getOrDefault(fqdn, 0);

                if (saltCount < cloudCount) {
                    LOGGER.info("Found unmounted disks for {} in group {}. Cloud: {}, Salt: {}", fqdn, group, cloudCount, saltCount);
                    eventService.fireCloudbreakEvent(stack.getId(), stack.getDetailedStatus().name(), DISK_SYNC_VOLUME_MOUNT_MISMATCH_FOUND,
                            Arrays.asList(fqdn, group, String.valueOf(cloudCount), String.valueOf(saltCount)));
                    // Add Mounting logic here if needed
                }
            }
        }
    }
}
