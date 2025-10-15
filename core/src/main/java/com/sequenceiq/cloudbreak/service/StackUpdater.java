package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.usage.service.ProviderSyncStateChangeUsageSenderService;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;
import com.sequenceiq.common.model.ProviderSyncState;

@Component
public class StackUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdater.class);

    @Inject
    private StackService stackService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private UsageLoggingUtil usageLoggingUtil;

    @Inject
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    @Inject
    private ProviderSyncStateChangeUsageSenderService providerSyncStateChangeUsageSenderService;

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus) {
        return doUpdateStackStatus(stackId, detailedStatus.getStatus(), detailedStatus, "");
    }

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        return doUpdateStackStatus(stackId, detailedStatus.getStatus(), detailedStatus, statusReason);
    }

    public Stack updateStackStatusAndSetDetailedStatusToUnknown(Long stackId, Status status, String statusReason) {
        return doUpdateStackStatus(stackId, status, DetailedStackStatus.UNKNOWN, statusReason);
    }

    public void updateSecurityConfig(SecurityConfig securityConfig) {
        securityConfigService.save(securityConfig);
    }

    public Stack updateClusterProxyRegisteredFlag(Stack stack, boolean registered) {
        stack.setClusterProxyRegistered(registered);
        return stackService.save(stack);
    }

    public void updateStackVersion(Long stackId, String stackVersion) {
        stackService.updateRuntimeVersion(stackId, stackVersion);
    }

    public void updateJavaVersion(Long stackId, String javaVersion) {
        stackService.updateJavaVersion(stackId, javaVersion);
    }

    public void addProviderState(String resourceCrn, Long stackId, ProviderSyncState state) {
        boolean changed = stackService.addProviderSyncState(stackId, state);
        if (changed) {
            providerSyncStateChangeUsageSenderService.addProviderSyncState(state, resourceCrn);
        }
    }

    public void removeProviderStates(String resourceCrn, Long stackId, Set<ProviderSyncState> states) {
        boolean changed = stackService.removeProviderSyncStates(stackId, states);
        if (changed) {
            states.forEach(state -> providerSyncStateChangeUsageSenderService.removeProviderSyncState(state, resourceCrn));
        }
    }

    public void updateExternalDatabaseEngineVersion(Long stackId, String databaseVersion) {
        stackService.updateExternalDatabaseEngineVersion(stackId, databaseVersion);
    }

    private Stack doUpdateStackStatus(Long stackId, Status newStatus, DetailedStackStatus newDetailedStatus, String statusReason) {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        return doUpdateStackStatus(stack, newStatus, newDetailedStatus, statusReason);
    }

    private Stack doUpdateStackStatus(Stack stack, Status newStatus, DetailedStackStatus newDetailedStatus, String statusReason) {
        StackStatus actualStackStatus = stack.getStackStatus();
        LOGGER.info("Update stack status from: {}/{} to: {}/{} stack: {} reason: {}", actualStackStatus.getStatus(), actualStackStatus.getDetailedStackStatus(),
                newStatus, newDetailedStatus, stack.getId(), statusReason);
        if (actualStackStatus.getStatus().equals(newStatus) && actualStackStatus.getDetailedStackStatus().equals(newDetailedStatus)) {
            LOGGER.debug("New status is the same as previous status {}/{}, skip status update.",
                    actualStackStatus.getStatus(), actualStackStatus.getDetailedStackStatus());
            return stack;
        } else if (!stack.isDeleteCompleted()) {
            stack.setStackStatus(new StackStatus(stack, newStatus, statusReason, newDetailedStatus));
            Cluster cluster = stack.getCluster();
            if (newStatus.isRemovableStatus()) {
                InMemoryStateStore.deleteStack(stack.getId());
                if (cluster != null) {
                    InMemoryStateStore.deleteCluster(cluster.getId());
                }
            } else {
                InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(newStatus));
                if (cluster != null) {
                    InMemoryStateStore.putCluster(cluster.getId(), statusToPollGroupConverter.convert(newStatus));
                }
            }
            stack = stackService.save(stack);
            saveDeprecatedClusterStatus(statusReason, stack, newStatus);
            usageLoggingUtil.logClusterStatusChangeUsageEvent(actualStackStatus.getStatus(), newStatus, cluster);
        } else {
            LOGGER.info("Stack is in DELETE_COMPLETED status, cannot update status.");
        }
        return stack;
    }

    public void updateVariant(Long resourceId, String variant) {
        CloudPlatformVariant stackVariant = stackService.getPlatformVariantByStackId(resourceId);
        if (!variant.equals(stackVariant.getVariant().value())) {
            Stack stack = stackService.get(resourceId);
            stack.setPlatformVariant(variant);
            stackService.save(stack);
        } else {
            LOGGER.info("The variant was already set to {}", variant);
        }
    }

    private void saveDeprecatedClusterStatus(String statusReason, Stack stack, Status newStatus) {
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            Status previous = cluster.getStatus();
            LOGGER.debug("Update deprecated cluster status from: {} to: {} reason: {} cluster: {}", previous, newStatus, statusReason, cluster.getId());
            cluster.setStatus(newStatus);
            cluster.setStatusReason(statusReason);
            clusterService.save(cluster);
        }
    }

    public void updateStackStatus(String resourceCrn, DetailedStackStatus detailedStackStatus, String statusReason) {
        Stack stack = stackService.getByCrn(resourceCrn);
        doUpdateStackStatus(stack, detailedStackStatus.getStatus(), detailedStackStatus, statusReason);
    }

    public void updateSupportedImdsVersionIfNecessary(Long stackId, InstanceMetadataUpdateType updateType) {
        Stack stack = stackService.get(stackId);
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        instanceMetadataUpdateProperties.validateUpdateType(updateType, cloudPlatform);
        Map<CloudPlatform, InstanceMetadataUpdateTypeMetadata> metadata = instanceMetadataUpdateProperties.getTypes().get(updateType).metadata();
        String supportedImdsVersion = metadata.get(cloudPlatform).imdsVersion();
        if (!StringUtils.equals(stack.getSupportedImdsVersion(), supportedImdsVersion)) {
            stack.setSupportedImdsVersion(supportedImdsVersion);
            stackService.save(stack);
        }
    }
}
