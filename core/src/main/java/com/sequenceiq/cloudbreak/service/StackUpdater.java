package com.sequenceiq.cloudbreak.service;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackBase;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterBase;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;

@Component
public class StackUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdater.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private UsageLoggingUtil usageLoggingUtil;

    @Inject
    private TransactionService transactionService;

    public void updateStackStatus(Long stackId, DetailedStackStatus detailedStatus) {
        doUpdateStackStatus(stackId, detailedStatus.getStatus(), detailedStatus, "");
    }

    public void updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        doUpdateStackStatus(stackId, detailedStatus.getStatus(), detailedStatus, statusReason);
    }

    public void updateStackStatusAndSetDetailedStatusToUnknown(Long stackId, Status status) {
        doUpdateStackStatus(stackId, status, DetailedStackStatus.UNKNOWN, "");
    }

    public void updateStackStatusAndSetDetailedStatusToUnknown(Long stackId, Status status, String statusRreason) {
        doUpdateStackStatus(stackId, status, DetailedStackStatus.UNKNOWN, statusRreason);
    }

    public void updateStackSecurityConfig(Stack stack, SecurityConfig securityConfig) {
        securityConfig = securityConfigService.save(securityConfig);
        stack.setSecurityConfig(securityConfig);
        stackService.save(stack);
    }

    public Stack updateClusterProxyRegisteredFlag(Stack stack, boolean registered) {
        stack.setClusterProxyRegistered(registered);
        return stackService.save(stack);
    }

    public Stack updateStackVersion(Long stackId, String stackVersion) {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        stack.setStackVersion(stackVersion);
        return stackService.save(stack);
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

    private void doUpdateStackStatus(Long stackId, Status newStatus, DetailedStackStatus newDetailedStatus, String statusReason) {
        try {
            StackBase stackBase = transactionService.required(() -> {
                StackBase stack = stackService.getStackBaseById(stackId);
                StackStatus<StackBase> actualStackStatus = stack.getStackStatus();
                LOGGER.info("Update stack status from: {}/{} to: {}/{} stack: {} reason: {}", actualStackStatus.getStatus(),
                        actualStackStatus.getDetailedStackStatus(),
                        newStatus, newDetailedStatus, stackId, statusReason);
                if (Objects.equals(actualStackStatus.getStatus(), newStatus)) {
                    LOGGER.debug("New status is the same as previous status {}/{}, skip status update.",
                            actualStackStatus.getStatus(), actualStackStatus.getDetailedStackStatus());
                    return null;
                } else if (Status.DELETE_COMPLETED.equals(actualStackStatus.getStatus())) {
                    LOGGER.info("Stack is in DELETE_COMPLETED status, cannot update status.");
                    return null;
                } else {
                    StackStatus<StackBase> stackStatus = new StackStatus<>(stack, newStatus, statusReason, newDetailedStatus);
                    stack.setStackStatus(stackStatus);
                    StackBase savedStack = stackService.update(stack);
                    saveDeprecatedClusterStatus(newStatus, statusReason, stack.getCluster());
                    if (newStatus.isRemovableStatus()) {
                        InMemoryStateStore.deleteStack(stackId);
                        if (stack.getCluster() != null) {
                            InMemoryStateStore.deleteCluster(stack.getCluster().getId());
                        }
                    } else {
                        InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(newStatus));
                        if (stack.getCluster() != null) {
                            InMemoryStateStore.putCluster(stack.getCluster().getId(), statusToPollGroupConverter.convert(newStatus));
                        }
                    }
                    return savedStack;
                }
            });
            if (stackBase != null) {
                usageLoggingUtil.logClusterStatusChangeUsageEvent(stackBase.getStackStatus().getStatus(), newStatus, stackBase);
            }
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void saveDeprecatedClusterStatus(Status newStatus, String statusReason, ClusterBase cluster) {
        if (cluster != null) {
            Status previous = cluster.getStatus();
            cluster.setStatus(newStatus);
            cluster.setStatusReason(statusReason);
            LOGGER.debug("Update deprecated cluster status from: {} to: {} reason: {} cluster: {}", previous, newStatus, statusReason, cluster.getId());
            clusterService.update(cluster);
        }
    }

}
