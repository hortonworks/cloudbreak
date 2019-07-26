package com.sequenceiq.cloudbreak.util;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.usage.LoggingUsageReporter;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

public class UsageLoggingUtil {

    private UsageReporter usageReporter = new LoggingUsageReporter();

    public void logClusterRequestedUsageEvent(Cluster cluster) {
        long timestamp = System.currentTimeMillis();
        // Datahub clusters will have datalake resource id set.
        if (cluster.getStack().getDatalakeResourceId() != null) {
            UsageProto.CDPDatahubClusterRequested proto = UsageProto.CDPDatahubClusterRequested.newBuilder()
                    .setAccountId(cluster.getStack().getCreator().getTenant().getName())
                    .setClusterId(cluster.getId().toString())
                    .setClusterName(cluster.getName())
                    .setCrn(cluster.getStack().getResourceCrn())
                    .setEnvironmentCrn(cluster.getStack().getEnvironmentCrn())
                    .setCreatorCrn(cluster.getStack().getCreator().getUserCrn())
                    .setDatalakeCrn(cluster.getStack().getDatalakeResourceId().toString())
                    .build();
            usageReporter.cdpDatahubClusterRequested(timestamp, proto);
        } else {
            UsageProto.CDPDatalakeClusterRequested proto = UsageProto.CDPDatalakeClusterRequested.newBuilder()
                    .setAccountId(cluster.getStack().getCreator().getTenant().getName())
                    .setDatalakeId(cluster.getId().toString())
                    .setDatalakeName(cluster.getName())
                    .setCrn(cluster.getStack().getResourceCrn())
                    .setEnvironmentCrn(cluster.getStack().getEnvironmentCrn())
                    .setCreatorCrn(cluster.getStack().getCreator().getUserCrn())
                    .build();
            usageReporter.cdpDatalakeClusterRequested(timestamp, proto);
        }
    }

    public void logClusterStatusChangeUsageEvent(Status oldClusterStatus, Cluster cluster) {
        // Datahub clusters will have datalake resource id set.
        if (cluster.getStack().getDatalakeResourceId() != null) {
            UsageProto.CDPDatahubClusterStatusChanged proto = UsageProto.CDPDatahubClusterStatusChanged.newBuilder()
                    .setClusterId(cluster.getId().toString())
                    .setOldStatus(UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(oldClusterStatus.name()))
                    .setNewStatus(UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(cluster.getStatus().name()))
                    .build();
            usageReporter.cdpDatahubClusterStatusChanged(proto);
        } else {
            UsageProto.CDPDatalakeClusterStatusChanged proto = UsageProto.CDPDatalakeClusterStatusChanged.newBuilder()
                    .setDatalakeId(cluster.getId().toString())
                    .setOldStatus(UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(oldClusterStatus.name()))
                    .setNewStatus(UsageProto.CDPCloudbreakClusterStatus.Value.valueOf(cluster.getStatus().name()))
                    .build();
            usageReporter.cdpDatalakeClusterStatusChanged(proto);
        }
    }
}
