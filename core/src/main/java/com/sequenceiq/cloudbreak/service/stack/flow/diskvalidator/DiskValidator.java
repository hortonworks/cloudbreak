package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_MISSING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_MISSING_BY_SIZE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_MOUNT_MISSING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_SIZE_MISMATCH;
import static com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricTag.ISSUE_TYPE;
import static com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricTag.PLATFORM_VARIANT;
import static com.sequenceiq.cloudbreak.service.metrics.MetricType.VOLUME_MOUNT_MISSING;
import static com.sequenceiq.cloudbreak.service.metrics.MetricType.VOLUME_MOUNT_SIZE_MISMATCH;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class DiskValidator {

    public static final String VOLUMES_INADEQUATE_EVENT_TYPE = "VOLUMES_INADEQUATE";

    public static final String GIGABYTE = " GiB";

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskValidator.class);

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private LsblkFetcher lsblkFetcher;

    @Inject
    private VolumeIdWithDeviceFetcher volumeIdWithDeviceFetcher;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private MetricService metricService;

    public void validateDisks(Stack stack, Set<Node> nodes) throws CloudbreakOrchestratorFailedException {
        List<Resource> diskResources = stack.getDiskResources();
        MultiValuedMap<String, VolumeInfo> volumeInfos = getVolumeInfos(diskResources);

        Set<String> hostNames = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        MultiValuedMap<String, LsblkLine> lsblkResults = lsblkFetcher.getLsblkResults(allGatewayConfigs, hostNames);

        MultiValuedMap<String, VolumeIdWithDevice> volumeMappings =
                volumeIdWithDeviceFetcher.getVolumeMappings(allGatewayConfigs, hostNames,
                        new CloudPlatformVariant(stack.cloudPlatform(), stack.getPlatformVariant()));

        for (Node node : nodes) {
            validateNodeVolumes(node, volumeInfos, stack, lsblkResults, volumeMappings);
        }
    }

    private MultiValuedMap<String, VolumeInfo> getVolumeInfos(List<Resource> diskResources) {
        MultiValuedMap<String, VolumeInfo> volumeInfos = new ArrayListValuedHashMap<>();
        for (Resource diskResource : diskResources) {
            resourceAttributeUtil.getTypedAttributes(diskResource, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
                    volumeInfos.put(diskResource.getInstanceId(), new VolumeInfo(volume.getId(), volume.getDevice(), volume.getSize().toString(),
                            volume.getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE));
                }
            });
        }
        return volumeInfos;
    }

    private void validateNodeVolumes(Node node, MultiValuedMap<String, VolumeInfo> volumeInfos, Stack stack, MultiValuedMap<String, LsblkLine> lsblkResults,
            MultiValuedMap<String, VolumeIdWithDevice> volumeMappings) {

        String instanceId = node.getInstanceId();
        String fqdn = node.getHostname();
        Collection<LsblkLine> lsblkLine = lsblkResults.get(fqdn);
        Collection<VolumeInfo> requiredVolumes = volumeInfos.get(instanceId);

        if (!volumeMappings.isEmpty()) {
            for (VolumeInfo volumeInfo : requiredVolumes) {
                validateVolume(stack, volumeInfo, fqdn, lsblkLine, volumeMappings.get(fqdn));
            }
        } else {
            validateVolumeCount(stack, requiredVolumes, fqdn, lsblkLine);
        }
    }

    private void validateVolume(Stack stack, VolumeInfo volumeInfo, String fqdn, Collection<LsblkLine> lsblkLines,
            Collection<VolumeIdWithDevice> volumeMappingsForFQDN) {
        if (volumeMappingsForFQDN != null) {
            volumeMappingsForFQDN.stream().filter(volumeMapping -> volumeInfo.getId().equals(volumeMapping.getVolumeId())).findFirst()
                    .ifPresentOrElse(volumeMapping -> validateVolumeSize(stack, volumeMapping, lsblkLines, volumeInfo, fqdn),
                            () -> logVolumeNotFound(stack, volumeInfo, fqdn));
        } else {
            logVolumeNotFound(stack, volumeInfo, fqdn);
        }
    }

    private void validateVolumeSize(Stack stack, VolumeIdWithDevice volumeIdWithDevice, Collection<LsblkLine> lsblkLines, VolumeInfo volumeInfo, String fqdn) {
        lsblkLines.stream().filter(line -> volumeIdWithDevice.getDevice().equals(line.getDevice())).findFirst()
                .ifPresentOrElse(line -> checkVolumeSizeAndMount(stack, line, volumeInfo, fqdn), () -> logVolumeNotFound(stack, volumeInfo, fqdn));
    }

    private void validateVolumeCount(Stack stack, Collection<VolumeInfo> requiredVolumes, String fqdn, Collection<LsblkLine> lsblkLines) {
        List<VolumeInfo> notFoundVolumesBySize = new LinkedList<>(requiredVolumes);
        lsblkLines.forEach(lsblkLine -> {
            if (isMounted(lsblkLine)) {
                notFoundVolumesBySize.stream().filter(volumeInfo -> Objects.equals(lsblkLine.getSize(), volumeInfo.getSize())).findFirst()
                        .ifPresent(notFoundVolumesBySize::remove);
            } else {
                LOGGER.debug("Volume {} is not mounted on the instance {}", lsblkLine.getDevice(), fqdn);
            }
        });
        if (!notFoundVolumesBySize.isEmpty()) {
            String volumesMissing = notFoundVolumesBySize.stream().map(volumeInfo -> volumeInfo.getSize() + GIGABYTE).collect(Collectors.joining(", "));
            LOGGER.warn("Missing volume(s) on {}: {}", fqdn, volumesMissing);
            metricService.incrementMetricCounter(VOLUME_MOUNT_MISSING,
                    ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING_BY_SIZE.name(),
                    PLATFORM_VARIANT.name(), stack.getPlatformVariant());
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE, CLUSTER_VOLUME_MISSING_BY_SIZE,
                    List.of(fqdn, volumesMissing));
        }
    }

    private void checkVolumeSizeAndMount(Stack stack, LsblkLine lsblkLine, VolumeInfo volumeInfo, String fqdn) {
        if (!volumeInfo.getSize().equals(lsblkLine.getSize())) {
            LOGGER.warn("Volume {} size mismatch for instance {}. Expected: {}, Actual: {}",
                    volumeInfo.getId(), fqdn, volumeInfo.getSize(), lsblkLine.getSize());
            metricService.incrementMetricCounter(VOLUME_MOUNT_SIZE_MISMATCH,
                    PLATFORM_VARIANT.name(), stack.getPlatformVariant());
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE, CLUSTER_VOLUME_SIZE_MISMATCH,
                    List.of(volumeInfo.getId(), fqdn, volumeInfo.getSize() + GIGABYTE, lsblkLine.getSize() + GIGABYTE));
        }
        if (!isMounted(lsblkLine)) {
            LOGGER.warn("Volume {} is not mounted, but it should have been on {}", volumeInfo.getId(), fqdn);
            metricService.incrementMetricCounter(VOLUME_MOUNT_MISSING,
                    ISSUE_TYPE.name(), CLUSTER_VOLUME_MOUNT_MISSING.name(),
                    PLATFORM_VARIANT.name(), stack.getPlatformVariant());
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE, CLUSTER_VOLUME_MOUNT_MISSING,
                    List.of(volumeInfo.getId(), fqdn));
        }
    }

    private boolean isMounted(LsblkLine lsblkLine) {
        return !Strings.isNullOrEmpty(lsblkLine.getMountPoint());
    }

    private void logVolumeNotFound(Stack stack, VolumeInfo volumeInfo, String fqdn) {
        LOGGER.warn("Volume {} not found on the instance {}", volumeInfo.getId(), fqdn);
        metricService.incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(),
                VOLUMES_INADEQUATE_EVENT_TYPE, CLUSTER_VOLUME_MISSING, List.of(volumeInfo.getId(), fqdn));
    }

}
