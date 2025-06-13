package com.sequenceiq.freeipa.service.stack;

import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.message.StackStatusMessageTransformator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.SecurityConfigService;

@Component
public class StackUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdater.class);

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private StackStatusMessageTransformator stackStatusMessageTransformator;

    @Inject
    private ServiceStatusRawMessageTransformer serviceStatusRawMessageTransformer;

    @Inject
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        return doUpdateStackStatus(stackId, detailedStatus, statusReason);
    }

    public Stack updateStackStatus(Stack stack, DetailedStackStatus detailedStatus, String statusReason) {
        try {
            return doUpdateStackStatus(stack, detailedStatus, statusReason);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.warn("Updating stack [{}] status to [{} - {}] failed due to [{}]. Retrying with refetching stack",
                    stack.getResourceCrn(), detailedStatus, statusReason, e.getMessage(), e);
            return doUpdateStackStatus(stack.getId(), detailedStatus, statusReason);
        }
    }

    public Stack updateStackSecurityConfig(Stack stack, SecurityConfig securityConfig) {
        securityConfig = securityConfigService.save(securityConfig);
        stack.setSecurityConfig(securityConfig);
        return stackService.save(stack);
    }

    public Stack updateClusterProxyRegisteredFlag(Stack stack, boolean registered) {
        stack.setClusterProxyRegistered(registered);
        return stackService.save(stack);
    }

    private Stack doUpdateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        Stack stack = stackService.getStackById(stackId);
        return doUpdateStackStatus(stack, detailedStatus, statusReason);
    }

    private Stack doUpdateStackStatus(Stack stack, DetailedStackStatus newDetailedStatus, String rawNewStatusReason) {
        Status newStatus = newDetailedStatus.getStatus();
        StackStatus stackStatus = stack.getStackStatus();
        if (!Status.DELETE_COMPLETED.equals(stackStatus.getStatus())) {
            rawNewStatusReason = serviceStatusRawMessageTransformer.transformMessage(rawNewStatusReason, stack.getTunnel());
            String transformedStatusReason = stackStatusMessageTransformator.transformMessage(rawNewStatusReason);
            if (isStatusChanged(stack, newDetailedStatus, transformedStatusReason, newStatus)) {
                stack = handleStatusChange(stack, newDetailedStatus, transformedStatusReason, newStatus, stackStatus);
            } else {
                LOGGER.debug("Statuses are the same, it will not update");
                updateInMemoryStoreIfStackIsMissing(stack, newStatus);
            }
        }
        return stack;
    }

    private void updateInMemoryStoreIfStackIsMissing(Stack stack, Status newStatus) {
        if (InMemoryStateStore.getStack(stack.getId()) == null) {
            LOGGER.debug("Although status hasn't changed, the stack is missing from 'InMemoryStateStore'. Updating 'InMemoryStateStore'");
            updateInMemoryStore(stack, newStatus);
        }
    }

    private Stack handleStatusChange(Stack stack, DetailedStackStatus newDetailedStatus, String newStatusReason, Status newStatus, StackStatus stackStatus) {
        stack = saveStackNewStatus(stack, newDetailedStatus, newStatusReason, newStatus, stackStatus);
        updateInMemoryStore(stack, newStatus);
        return stack;
    }

    private void updateInMemoryStore(Stack stack, Status newStatus) {
        if (newStatus.isRemovableStatus()) {
            InMemoryStateStore.deleteStack(stack.getId());
        } else {
            PollGroup pollGroup = Status.DELETE_COMPLETED.equals(newStatus) ? PollGroup.CANCELLED : PollGroup.POLLABLE;
            InMemoryStateStore.putStack(stack.getId(), pollGroup);
        }
    }

    private Stack saveStackNewStatus(Stack stack, DetailedStackStatus newDetailedStatus, String newStatusReason, Status newStatus, StackStatus stackStatus) {
        LOGGER.debug("Updated: status from {} to {} - detailed status from {} to {} - reason from {} to {}",
                stackStatus.getStatus(), newStatus, stackStatus.getDetailedStackStatus(), newDetailedStatus,
                stackStatus.getStatusReason(), newStatusReason);
        stack.setStackStatus(new StackStatus(stack, newStatus, newStatusReason, newDetailedStatus));
        stack = stackService.save(stack);
        return stack;
    }

    private boolean isStatusChanged(Stack stack, DetailedStackStatus detailedStatus, String statusReason, Status status) {
        return status != stack.getStackStatus().getStatus()
                || !detailedStatus.equals(stack.getStackStatus().getDetailedStackStatus())
                || !Objects.equals(statusReason, stack.getStackStatus().getStatusReason());
    }

    public void updateVariant(Long resourceId, String variant) {
        LOGGER.debug("Update variant for stack to variant of {}", variant);
        Stack stack = stackService.getStackById(resourceId);
        if (!variant.equals(stack.getPlatformvariant())) {
            LOGGER.debug("The new stack variant and old stack variant are different, update it");
            stack.setPlatformvariant(variant);
            stackService.save(stack);
        } else {
            LOGGER.info("The variant was already set to {}", variant);
        }
    }

    public void updateSupportedImdsVersion(Long stackId, InstanceMetadataUpdateType updateType) {
        Stack stack = stackService.getStackById(stackId);
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
