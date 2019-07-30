package com.sequenceiq.cloudbreak.util;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.usage.LoggingUsageReporter;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.cloudbreak.workspace.model.User;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPCloudbreakClusterStatus.Value.UNSET;

/**
 * Utility class for logging usage events.
 */
@Component
public class UsageLoggingUtil {

    private UsageReporter usageReporter = new LoggingUsageReporter();

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
        User creator = stack.getCreator();
        String cloudPlatform = stack.getCloudPlatform();
        UsageProto.CDPEnvironmentsEnvironmentType.Value cloudPlatformEnum =
                UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET;
        if (cloudPlatform != null) {
            cloudPlatformEnum = UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(cloudPlatform);
        }
        if (stack.getDatalakeResourceId() != null) {
            UsageProto.CDPDatahubClusterRequested.Builder protoBuilder =
                    UsageProto.CDPDatahubClusterRequested.newBuilder();
            protoBuilder.setClusterId(cluster.getId().toString());
            protoBuilder.setDatalakeCrn(stack.getDatalakeResourceId().toString());
            if (creator != null) {
                if (creator.getTenant() != null && creator.getTenant().getName() != null) {
                    protoBuilder.setAccountId(creator.getTenant().getName());
                }
                if (creator.getUserCrn() != null) {
                    protoBuilder.setCreatorCrn(creator.getUserCrn());
                }
            }
            if (cluster.getName() != null) {
                protoBuilder.setClusterName(cluster.getName());
            }
            if (stack.getResourceCrn() != null) {
                protoBuilder.setCrn(stack.getResourceCrn());
            }
            if (stack.getEnvironmentCrn() != null) {
                protoBuilder.setEnvironmentCrn(stack.getEnvironmentCrn());
            }
            if (cloudPlatformEnum != UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET) {
                protoBuilder.setEnvironmentType(cloudPlatformEnum);
            }
            usageReporter.cdpDatahubClusterRequested(timestamp, protoBuilder.build());
        } else {
            UsageProto.CDPDatalakeClusterRequested.Builder protoBuilder =
                    UsageProto.CDPDatalakeClusterRequested.newBuilder();
            protoBuilder.setDatalakeId(cluster.getId().toString());
            if (creator != null) {
                if (creator.getTenant() != null && creator.getTenant().getName() != null) {
                    protoBuilder.setAccountId(creator.getTenant().getName());
                }
                if (creator.getUserCrn() != null) {
                    protoBuilder.setCreatorCrn(creator.getUserCrn());
                }
            }
            if (cluster.getName() != null) {
                protoBuilder.setDatalakeName(cluster.getName());
            }
            if (stack.getResourceCrn() != null) {
                protoBuilder.setCrn(stack.getResourceCrn());
            }
            if (stack.getEnvironmentCrn() != null) {
                protoBuilder.setEnvironmentCrn(stack.getEnvironmentCrn());
            }
            if (cloudPlatformEnum != UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET) {
                protoBuilder.setEnvironmentType(cloudPlatformEnum);
            }
            usageReporter.cdpDatalakeClusterRequested(timestamp, protoBuilder.build());
        }
    }

    /**
     * Log datalake/datahub status change usage event.
     * @param oldClusterStatus the old cluster status. Nullable. If null, no usage events will be logged.
     * @param cluster the cluster object. Nullable. If null, no usage events will be logged.
     */
    public void logClusterStatusChangeUsageEvent(@Nullable Status oldClusterStatus, @Nullable Cluster cluster) {
        if (oldClusterStatus == null || cluster == null || cluster.getId() == null || cluster.getStatus() == null) {
            return;
        }
        Stack stack = cluster.getStack();
        if (stack == null) {
            return;
        }
        UsageProto.CDPCloudbreakClusterStatus.Value oldStatusEnum = UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(oldClusterStatus.name());
        UsageProto.CDPCloudbreakClusterStatus.Value newStatusEnum = UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(cluster.getStatus().name());
        if (oldStatusEnum == UNSET || newStatusEnum == UNSET) {
            return;
        }
        // Datahub clusters will have datalake resource id set.
        if (stack.getDatalakeResourceId() != null) {
            UsageProto.CDPDatahubClusterStatusChanged proto = UsageProto.CDPDatahubClusterStatusChanged.newBuilder()
                    .setClusterId(cluster.getId().toString())
                    .setOldStatus(oldStatusEnum)
                    .setNewStatus(newStatusEnum)
                    .build();
            usageReporter.cdpDatahubClusterStatusChanged(proto);
        } else {
            UsageProto.CDPDatalakeClusterStatusChanged proto = UsageProto.CDPDatalakeClusterStatusChanged.newBuilder()
                    .setDatalakeId(cluster.getId().toString())
                    .setOldStatus(oldStatusEnum)
                    .setNewStatus(newStatusEnum)
                    .build();
            usageReporter.cdpDatalakeClusterStatusChanged(proto);
        }
    }
}
