package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class DatabaseUpgradeBackupRestoreChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUpgradeBackupRestoreChecker.class);

    @Value("${cb.db.env.upgrade.rds.backuprestore.cloudplatforms}")
    private Set<CloudPlatform> cloudPlatformsToRunBackupRestore;

    public boolean shouldRunDataBackupRestore(StackView stack, ClusterView cluster, Database database) {
        boolean platformSupported = cloudPlatformsToRunBackupRestore.contains(CloudPlatform.valueOf(stack.getCloudPlatform()));
        boolean embeddedDatabaseOnAttachedDisk = cluster.getEmbeddedDatabaseOnAttachedDisk();
        boolean databaseTypeRequiresBackupRestore = databaseTypeRequiresBackupRestore(stack, database);

        LOGGER.debug("Running backup and restore based on conditions: " +
                        "platformSupported: {}, " +
                        "embeddedDatabaseOnAttachedDisk: {}, " +
                        "databaseTypeRequiresBackupRestore: {}",
                platformSupported, embeddedDatabaseOnAttachedDisk, databaseTypeRequiresBackupRestore);
        return platformSupported && !embeddedDatabaseOnAttachedDisk && databaseTypeRequiresBackupRestore;
    }

    private boolean databaseTypeRequiresBackupRestore(StackView stack, Database database) {
        Json attributes = database.getAttributes();
        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
        if (stack.getCloudPlatform().equals(CloudPlatform.AZURE.name())) {
            String dbTypeStr = (String) params.get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
            AzureDatabaseType azureDatabaseType =
                    StringUtils.isNotBlank(dbTypeStr) ? AzureDatabaseType.valueOf(dbTypeStr) : AzureDatabaseType.SINGLE_SERVER;
            return azureDatabaseType == AzureDatabaseType.SINGLE_SERVER;
        }
        return false;
    }
}