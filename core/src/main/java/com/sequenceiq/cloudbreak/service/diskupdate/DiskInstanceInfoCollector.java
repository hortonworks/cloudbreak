package com.sequenceiq.cloudbreak.service.diskupdate;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.LUN_DEVICE_PATH_PREFIX;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator.LsblkFetcher;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class DiskInstanceInfoCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskInstanceInfoCollector.class);

    private static final int LUN_INDEX = 3;

    @Inject
    private LsblkFetcher lsblkFetcher;

    @Inject
    private ResourceSyncUtil resourceSyncUtil;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudConnectorHelper cloudConnectorHelper;

    public Map<String, InstanceResourceDto> getAndParseSaltInfo(StackDto stackDto,
        Map<String, String> fqdnInstanceIdMap, Map<String, List<VolumeRecord>> cloudMetadata, String cloudPlatform)
        throws CloudbreakOrchestratorFailedException {
        Map<String, String> saltFstabInfo = resourceSyncUtil.getFstabInformation(stackDto.getId());
        LOGGER.info("Collected fstab for stack: {}, fstab - {}", stackDto.getId(), saltFstabInfo);
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
        Set<String> allNodes = stackUtil.collectNodes(stackDto).stream().map(Node::getHostname).collect(Collectors.toSet());
        MultiValuedMap<String, InstanceResourceDto.VolumeDto> saltLsblkInfo = lsblkFetcher.getLsblkResults(allGatewayConfigs, allNodes);
        LOGGER.info("Collected lsblk for stack: {}, fstab - {}", stackDto.getId(), saltLsblkInfo);
        return saltFstabInfo.keySet().stream().map(key -> {
            Collection<InstanceResourceDto.VolumeDto> lsblkLines = saltLsblkInfo.get(key);
            String fstabFromInstance = saltFstabInfo.get(key).trim();
            String instanceId = fqdnInstanceIdMap.get(key);
            if (instanceId == null) {
                return null;
            }
            InstanceResourceDto instanceResourceDto = getInstanceResourceDto(lsblkLines, cloudMetadata, instanceId, cloudPlatform);
            instanceResourceDto.setFstab(fstabFromInstance);
            return instanceResourceDto;
        })
        .filter(Objects::nonNull)
        .collect(toMap(InstanceResourceDto::getInstanceId, d -> d));
    }

    @VisibleForTesting
    InstanceResourceDto getInstanceResourceDto(Collection<InstanceResourceDto.VolumeDto> lsblkLines,
        Map<String, List<VolumeRecord>> cloudMetadata, String instanceId, String cloudPlatform) {
        InstanceResourceDto instanceResourceDto = new InstanceResourceDto();
        List<InstanceResourceDto.VolumeDto> volumesFromLsblk = filter(lsblkLines);

        Map<String, InstanceResourceDto.VolumeDto> diskMap = volumesFromLsblk.stream()
                .collect(toMap(d -> {
                    if (cloudPlatform.equals(CloudPlatform.AZURE.toString()) && d.hctl() != null) {
                        return LUN_DEVICE_PATH_PREFIX + d.hctl().split(":")[LUN_INDEX];
                    }
                    if (cloudPlatform.equals(CloudPlatform.AWS.toString()) && d.serial() != null) {
                        return d.serial().replaceFirst("^vol", "vol-");
                    }
                    return d.deviceName();
                }, d -> d));

        instanceResourceDto.setInstanceId(instanceId);

        List<InstanceResourceDto.VolumeDto> updatedVolumes = new ArrayList<>();
        for (VolumeRecord vol : cloudMetadata.getOrDefault(instanceId, Collections.emptyList())) {
            String key = cloudPlatform.equals(CloudPlatform.AWS.toString()) ? vol.id() : vol.device();
            if (diskMap.containsKey(key)) {
                InstanceResourceDto.VolumeDto existing = diskMap.get(key);
                InstanceResourceDto.VolumeDto updatedVolume = new InstanceResourceDto.VolumeDto(vol.id(),
                        vol.device(), existing.mountPoint(), existing.size(), vol.type(), existing.uuid(),
                        existing.serial(), existing.hctl());
                updatedVolumes.add(updatedVolume);
            }
        }
        instanceResourceDto.setVolumes(updatedVolumes);
        return instanceResourceDto;
    }

    private List<InstanceResourceDto.VolumeDto> filter(Collection<InstanceResourceDto.VolumeDto> lsblkLines) {
        return lsblkLines.stream()
            .filter(line -> line.uuid() != null && !"".equals(line.uuid()))
            .filter(line -> line.mountPoint().contains("/hadoopfs/") || line.mountPoint().contains("/dbfs"))
            .filter(line -> "disk".equalsIgnoreCase(line.volumeType()))
            .filter(line -> line.hctl() != null || line.serial() != null)
            .collect(Collectors.toList());
    }

    public Map<String, List<VolumeRecord>> getCloudMetadataMap(StackDto stack) {
        CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
        CloudConnector cloudConnector = cloudConnectResources.getCloudConnector();
        List<String> instanceIds = stack.getInstanceGroupDtos().stream().flatMap(i -> i.getNotTerminateAndNotZombieInstanceMetaData().stream())
                .map(InstanceMetadataView::getInstanceId).toList();
        return cloudConnector.volumeConnector().describeAttachedVolumes(cloudConnectResources.getAuthenticatedContext(),
                cloudConnectResources.getCloudStack(), instanceIds);
    }
}
