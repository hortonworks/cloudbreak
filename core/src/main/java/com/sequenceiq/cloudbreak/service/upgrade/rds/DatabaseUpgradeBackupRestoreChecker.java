package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;

@Component
public class DatabaseUpgradeBackupRestoreChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUpgradeBackupRestoreChecker.class);

    @Value("${cb.db.env.upgrade.rds.backuprestore.cloudplatforms}")
    private Set<CloudPlatform> cloudPlatformsToRunBackupRestore;

    public boolean shouldRunDataBackupRestore(ClusterViewContext context) {
        boolean platformSupported = cloudPlatformsToRunBackupRestore.contains(CloudPlatform.valueOf(context.getStack().getCloudPlatform()));
        boolean embeddedDatabaseOnAttachedDisk = context.getCluster().getEmbeddedDatabaseOnAttachedDisk();
        LOGGER.debug("Running backup and restore based on conditions: platformSupported: {}, embeddedDatabaseOnAttachedDisk: {}",
                platformSupported, embeddedDatabaseOnAttachedDisk);
        return platformSupported && !embeddedDatabaseOnAttachedDisk;
    }
}
