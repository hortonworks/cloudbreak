package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.common.model.FileSystemType;

@Component
public class BackupRestoreDBStateParamsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreDBStateParamsProvider.class);

    private static final String EMBEDDED_DB_HOST_KEY = "embeddeddb_host";

    private static final String EMBEDDED_DB_PORT_KEY = "embeddeddb_port";

    private static final String EMBEDDED_DB_USER_KEY = "embeddeddb_user";

    private static final String EMBEDDED_DB_PASSWORD_KEY = "embeddeddb_password";

    private static final String BACKUP_LOCATION_KEY = "backup_location";

    private static final String BACKUP_INSTANCE_PROFILE = "backup_instance_profile";

    private static final String ABFS_ACCOUNT_NAME_KEY = "abfs_account_name";

    private static final String ABFS_FILE_SYSTEM_KEY = "abfs_file_system";

    private static final String ABFS_FILE_SYSTEM_FOLDER_KEY = "abfs_file_system_folder";

    private static final String TARGET_VERSION = "target_version";

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    public Map<String, Object> createParamsForBackupRestore(String backupLocation, String backupInstanceProfile) {
        return createParamsForBackupRestore(backupLocation, backupInstanceProfile, null);
    }

    public Map<String, Object> createParamsForBackupRestore(String backupLocation, String backupInstanceProfile, String targetVersion) {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> postgresParams = new HashMap<>();
        params.put("postgres", postgresParams);
        Map<String, String> upgradeParams = new HashMap<>();
        postgresParams.put("upgrade", upgradeParams);
        upgradeParams.put(EMBEDDED_DB_HOST_KEY, "localhost");
        upgradeParams.put(EMBEDDED_DB_PORT_KEY, "5432");
        upgradeParams.put(EMBEDDED_DB_USER_KEY, "postgres");
        upgradeParams.put(EMBEDDED_DB_PASSWORD_KEY, "postgres");
        if (StringUtils.isNotBlank(targetVersion)) {
            upgradeParams.put(TARGET_VERSION, targetVersion);
        }
        setCloudStorageBackupParameters(backupLocation, upgradeParams, backupInstanceProfile);
        LOGGER.debug("Created parameters for RDS upgrade backup and restore: {}", upgradeParams);
        return params;
    }

    private void setCloudStorageBackupParameters(String backupLocation, Map<String, String> upgradeParams, String backupInstanceProfile) {
        if (StringUtils.isNotBlank(backupLocation) && backupLocation.startsWith(FileSystemType.ADLS_GEN_2.getProtocol())) {
            AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(backupLocation);
            upgradeParams.put(BACKUP_LOCATION_KEY, backupLocation);
            upgradeParams.put(ABFS_ACCOUNT_NAME_KEY, adlsGen2Config.getAccount());
            upgradeParams.put(ABFS_FILE_SYSTEM_KEY, adlsGen2Config.getFileSystem());
            upgradeParams.put(ABFS_FILE_SYSTEM_FOLDER_KEY, adlsGen2Config.getFolderPrefix());
            upgradeParams.put(BACKUP_INSTANCE_PROFILE, backupInstanceProfile);
            LOGGER.debug("Pillars for cloud storage location {} have been set", backupLocation);
        }
    }
}
