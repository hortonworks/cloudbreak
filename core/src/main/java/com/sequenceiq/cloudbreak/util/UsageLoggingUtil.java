package com.sequenceiq.cloudbreak.util;

import javax.annotation.Nullable;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.stack.StackBase;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.usage.UsageReportProcessor;

/**
 * Utility class for logging usage events.
 */
@Component
public class UsageLoggingUtil {

    private final UsageReportProcessor usageReportProcessor;

    public UsageLoggingUtil(UsageReportProcessor usageReportProcessor) {
        this.usageReportProcessor = usageReportProcessor;
    }

    /**
     * Log datalake/datahub requested usage event.
     * @param cluster the cluster object. Nullable. If null, no usage events will be logged.
     */
    public void logClusterRequestedUsageEvent(@Nullable Cluster cluster) {
        if (cluster == null || cluster.getId() == null) {
            return;
        }
        Stack stack = cluster.getStack();
        if (stack == null) {
            return;
        }
        long timestamp = System.currentTimeMillis();
        // Datahub clusters will have datalake resource id set.
        String cloudPlatform = stack.getCloudPlatform();
        UsageProto.CDPEnvironmentsEnvironmentType.Value cloudPlatformEnum =
                UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET;
        if (cloudPlatform != null) {
            try {
                cloudPlatformEnum = UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(cloudPlatform.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Do not set the cloud platform.
            }
        }
        if (StackType.DATALAKE == stack.getType()) {
            UsageProto.CDPDatalakeClusterRequested.Builder protoBuilder =
                    UsageProto.CDPDatalakeClusterRequested.newBuilder();
            protoBuilder.setDatalakeId(cluster.getId().toString());
            buildDatalakeRequestedProto(cluster, cloudPlatformEnum, protoBuilder);
            usageReportProcessor.cdpDatalakeClusterRequested(timestamp, protoBuilder.build());
        } else {
            UsageProto.CDPDatahubClusterRequested.Builder protoBuilder =
                    UsageProto.CDPDatahubClusterRequested.newBuilder();
            protoBuilder.setClusterId(cluster.getId().toString());
            if (StringUtils.hasLength(stack.getDatalakeCrn())) {
                protoBuilder.setDatalakeCrn(stack.getDatalakeCrn());
            }
            buildDatahubRequestedProto(cluster, cloudPlatformEnum, protoBuilder);
            usageReportProcessor.cdpDatahubClusterRequested(timestamp, protoBuilder.build());
        }
    }

    /**
     * Log datalake/datahub status change usage event.
     * @param oldClusterStatus the old cluster status. Nullable. If null, no usage events will be logged.
     * @param newClusterStatus the new cluster status. Nullable. If null, no usage events will be logged.
     * @param stack the stack object. Nullable. If null, no usage events will be logged.
     */
    public void logClusterStatusChangeUsageEvent(@Nullable Status oldClusterStatus, @Nullable Status newClusterStatus, @Nullable StackBase stack) {
        if (oldClusterStatus == null || newClusterStatus == null || stack == null || stack.getCluster() == null || stack.getCluster().getId() == null) {
            return;
        }
        UsageProto.CDPCloudbreakClusterStatus.Value oldStatusEnum = null;
        UsageProto.CDPCloudbreakClusterStatus.Value newStatusEnum = null;
        try {
            oldStatusEnum = UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(
                    oldClusterStatus.name().toUpperCase());
            newStatusEnum = UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(
                    newClusterStatus.name().toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        if (oldStatusEnum == newStatusEnum) {
            return;
        }
        if (StackType.DATALAKE == stack.getType()) {
            UsageProto.CDPDatalakeClusterStatusChanged proto = UsageProto.CDPDatalakeClusterStatusChanged.newBuilder()
                    .setDatalakeId(stack.getCluster().getId().toString())
                    .setOldStatus(oldStatusEnum)
                    .setNewStatus(newStatusEnum)
                    .build();
            usageReportProcessor.cdpDatalakeClusterStatusChanged(proto);
        } else {
            UsageProto.CDPDatahubClusterStatusChanged proto = UsageProto.CDPDatahubClusterStatusChanged.newBuilder()
                    .setClusterId(stack.getCluster().getId().toString())
                    .setOldStatus(oldStatusEnum)
                    .setNewStatus(newStatusEnum)
                    .build();
            usageReportProcessor.cdpDatahubClusterStatusChanged(proto);
        }
    }

    private void buildDatalakeRequestedProto(
            Cluster cluster,
            UsageProto.CDPEnvironmentsEnvironmentType.Value cloudPlatformEnum,
            UsageProto.CDPDatalakeClusterRequested.Builder protoBuilder) {
        Stack stack = cluster.getStack();
        protoBuilder.setCreatorCrn(ThreadBasedUserCrnProvider.getUserCrn());
        if (cluster.getName() != null) {
            protoBuilder.setDatalakeName(cluster.getName());
        }
        if (stack.getResourceCrn() != null) {
            protoBuilder.setCrn(stack.getResourceCrn());
            protoBuilder.setAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
        }
        if (stack.getEnvironmentCrn() != null) {
            protoBuilder.setEnvironmentCrn(stack.getEnvironmentCrn());
        }
        protoBuilder.setEnvironmentType(cloudPlatformEnum);
        if (cluster.getBlueprint() != null && cluster.getBlueprint().getName() != null) {
            protoBuilder.setClusterDefinitionName(cluster.getBlueprint().getName());
        }
    }

    private void buildDatahubRequestedProto(
            Cluster cluster,
            UsageProto.CDPEnvironmentsEnvironmentType.Value cloudPlatformEnum,
            UsageProto.CDPDatahubClusterRequested.Builder protoBuilder) {
        Stack stack = cluster.getStack();
        protoBuilder.setCreatorCrn(ThreadBasedUserCrnProvider.getUserCrn());
        if (cluster.getName() != null) {
            protoBuilder.setClusterName(cluster.getName());
        }
        if (stack.getResourceCrn() != null) {
            protoBuilder.setCrn(stack.getResourceCrn());
            protoBuilder.setAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
        }
        if (stack.getEnvironmentCrn() != null) {
            protoBuilder.setEnvironmentCrn(stack.getEnvironmentCrn());
        }
        protoBuilder.setEnvironmentType(cloudPlatformEnum);
        if (cluster.getBlueprint() != null && cluster.getBlueprint().getName() != null) {
            protoBuilder.setClusterDefinitionName(cluster.getBlueprint().getName());
        }
    }
}
