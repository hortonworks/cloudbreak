package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
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
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private UsageLoggingUtil usageLoggingUtil;

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus) {
        return doUpdateStackStatus(stackId, detailedStatus.getStatus(), detailedStatus, "");
    }

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        return doUpdateStackStatus(stackId, detailedStatus.getStatus(), detailedStatus, statusReason);
    }

    public Stack updateStackStatusAndSetDetailedStatusToUnknown(Long stackId, Status status) {
        return doUpdateStackStatus(stackId, status, DetailedStackStatus.UNKNOWN, "");
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

    private Stack doUpdateStackStatus(Long stackId, Status newStatus, DetailedStackStatus newDetailedStatus, String statusReason) {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        StackStatus actualStackStatus = stack.getStackStatus();
        LOGGER.info("Update stack status from: {}/{} to: {}/{} stack: {} reason: {}", actualStackStatus.getStatus(), actualStackStatus.getDetailedStackStatus(),
                newStatus, newDetailedStatus, stackId, statusReason);
        if (actualStackStatus.getStatus().equals(newStatus)) {
            LOGGER.debug("New status is the same as previous status {}/{}, skip status update.",
                    actualStackStatus.getStatus(), actualStackStatus.getDetailedStackStatus());
            return stack;
        } else if (!stack.isDeleteCompleted()) {
            stack.setStackStatus(new StackStatus(stack, newStatus, statusReason, newDetailedStatus));
            Cluster cluster = stack.getCluster();
            if (newStatus.isRemovableStatus()) {
                InMemoryStateStore.deleteStack(stackId);
                if (cluster != null) {
                    InMemoryStateStore.deleteCluster(cluster.getId());
                }
            } else {
                InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(newStatus));
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

}
