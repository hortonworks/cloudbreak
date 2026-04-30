package com.sequenceiq.cloudbreak.job.diskusage;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class DiskUsageSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskUsageSyncService.class);

    @Inject
    private DiskUsageSyncConfig diskUsageSyncConfig;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private StackUtil stackUtil;

    public void checkDbDisk(StackDto stack) {
        try {
            if (!stackUtil.hasDiskResourcesWithDeprecatedDevicePaths(stack)) {
                int usage = checkDbDiskUsage(stack);
                LOGGER.debug("Database disk usage for stack: {} is {}%", stack.getResourceCrn(), usage);
                if (usage >= diskUsageSyncConfig.getDbDiskUsageThresholdPercentage()) {
                    LOGGER.warn("Database disk usage for stack: {} is at {}%, which is above the warning threshold of {}%",
                            stack.getResourceCrn(), usage, diskUsageSyncConfig.getDbDiskUsageThresholdPercentage());
                    resizeDbDisk(stack, usage);
                }
            } else {
                LOGGER.info("Stack {} has deprecated device paths, DB disk check and resize is skipped.", stack.getResourceCrn());
            }
        } catch (Exception e) {
            LOGGER.error("Error during disk usage sync, skipping and logging it: ", e);
        }
    }

    private int checkDbDiskUsage(StackDto stack) {
        LOGGER.debug("Checking DB Disk Usage for stack: {}", stack.getResourceCrn());
        Optional<String> primaryGatewayFQDN = stack.getPrimaryGatewayFQDN();
        if (primaryGatewayFQDN.isEmpty()) {
            LOGGER.warn("Cannot check database disk usage, primary gateway FQDN is not available for stack: {}", stack.getResourceCrn());
            return 0;
        }
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            Optional<Integer> databaseDiskUsagePercentage = hostOrchestrator.getDatabaseDiskUsagePercentage(primaryGatewayConfig, primaryGatewayFQDN.get());
            if (databaseDiskUsagePercentage.isPresent()) {
                return databaseDiskUsagePercentage.get();
            } else {
                LOGGER.warn("Database disk usage information is not available for stack: {}", stack.getResourceCrn());
            }
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Error while checking database disk usage for stack: {}", stack.getResourceCrn(), e);
        }
        return 0;
    }

    private void resizeDbDisk(StackDto stack, int usage) {
        DiskUpdateRequest updateRequest = new DiskUpdateRequest();
        updateRequest.setGroup(stack.getPrimaryGatewayGroup().getGroupName());
        updateRequest.setDiskType(DiskType.DATABASE_DISK);
        Integer currentSize = getCurrentSize(stack);
        if (currentSize != null) {
            Integer newSize = getNewSize(stack, currentSize, usage);
            if (newSize != null) {
                updateRequest.setSize(newSize);
                if (diskUsageSyncConfig.isDryRun()) {
                    LOGGER.info("[DRY-RUN] Embedded DB disk resize simulation for stack {}. " +
                                    "Actual usage is {}% of {}GB. In a real run, it would be increased to {}GB, " +
                                    "but now we are just testing the logic.",
                            stack.getResourceCrn(), usage, currentSize, newSize);
                } else {
                    logResizeEvent(stack.getId(), newSize);
                    flowManager.triggerStackUpdateDisks(stack, updateRequest);
                }
            }
        }
    }

    private void logResizeEvent(Long stackId, int newSize) {
        flowMessageService.fireEventAndLog(stackId,
                Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.DATAHUB_AUTOMATIC_DB_DISK_RESIZE_INITIATED,
                String.valueOf(diskUsageSyncConfig.getDbDiskUsageThresholdPercentage()),
                String.valueOf(newSize));
    }

    private Integer getCurrentSize(StackDto stack) {
        Set<VolumeTemplate> volumeTemplates = stack.getPrimaryGatewayGroup().getTemplate().getVolumeTemplates();
        Optional<VolumeTemplate> dbVolumeTemplate = volumeTemplates.stream()
                .filter(volumeTemplate -> volumeTemplate.getUsageType() == VolumeUsageType.DATABASE)
                .findFirst();
        if (dbVolumeTemplate.isEmpty()) {
            LOGGER.error("Database volume template not found for stack: {}, resizing not possible", stack.getResourceCrn());
            return null;
        }
        return dbVolumeTemplate.get().getVolumeSize();
    }

    private Integer getNewSize(StackDto stack, int currentSize, int usage) {
        int newSize = currentSize + diskUsageSyncConfig.getDiskIncrementSize();
        newSize = Math.min(newSize, diskUsageSyncConfig.getMaxDiskSize());
        if (newSize <= currentSize) {
            LOGGER.warn("Database disk size for stack: {} is already at or above the maximum size of {}GB, resizing not possible",
                    stack.getResourceCrn(), diskUsageSyncConfig.getMaxDiskSize());
            logMaxSizeReachedEvent(stack.getId(), currentSize, usage);
            return null;
        }
        LOGGER.info("Resizing database disk for stack: {} from {}GB to {}GB", stack.getResourceCrn(), currentSize, newSize);
        return newSize;
    }

    private void logMaxSizeReachedEvent(Long stackId, int currentSize, int usage) {
        flowMessageService.fireEventAndLog(stackId,
                Status.AVAILABLE.name(),
                ResourceEvent.DATAHUB_AUTOMATIC_DB_DISK_RESIZE_LIMIT_REACHED,
                String.valueOf(diskUsageSyncConfig.getMaxDiskSize()),
                String.valueOf(currentSize),
                String.valueOf(usage));
    }
}
