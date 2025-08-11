package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.MOCK;
import static java.util.Collections.singletonMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class BackupRestoreSaltConfigGenerator {
    // these values are tightly coupled to the `postgresql/disaster_recovery.sls` salt pillar in `orchestrator-salt`
    public static final String POSTGRESQL_DISASTER_RECOVERY_PILLAR_PATH = "/postgresql/disaster_recovery.sls";

    public static final String DISASTER_RECOVERY_KEY = "disaster_recovery";

    public static final String OBJECT_STORAGE_URL_KEY = "object_storage_url";

    public static final String DATABASE_BACKUP_POSTFIX = "_database_backup";

    public static final String RANGER_ADMIN_GROUP_KEY = "ranger_admin_group";

    public static final String CLOSE_CONNECTIONS = "close_connections";

    public static final String DATABASE_NAMES_KEY = "database_name";

    public static final String COMPRESSION_LEVEL = "compression_level";

    public static final String RAZ_ENABLED = "raz_enabled";

    public static final String TEMP_BACKUP_DIR = "temp_backup_dir";

    public static final String TEMP_RESTORE_DIR = "temp_restore_dir";

    public static final String BACKUP_RESTORE_CONFIG = "backup_restore_config";

    public static final String BACKUP_RESTORE_CONFIG_PATH = "/postgresql/backup_restore_config.sls";

    public static final List<DatabaseType> DEFAULT_BACKUP_DATABASE =
            List.of(DatabaseType.HIVE, DatabaseType.RANGER, DatabaseType.PROFILER_AGENT, DatabaseType.PROFILER_METRIC);

    public static final String DEFAULT_LOCAL_BACKUP_DIR = "/var/tmp";

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreSaltConfigGenerator.class);

    private static final String SKIP_COMPRESSION_VALUE = "0";

    private static final String FAST_COMPRESSION_VALUE = "1";

    @SuppressWarnings("ParameterNumber")
    public SaltConfig createSaltConfig(String location, String backupId, String rangerAdminGroup,
            boolean closeConnections, List<String> skipDatabaseNames, boolean enableCompression, StackView stack, boolean razEnabled)
            throws URISyntaxException {
        String fullLocation = buildFullLocation(location, backupId, stack.getCloudPlatform());

        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Map<String, String> disasterRecoveryValues = new HashMap<>();
        disasterRecoveryValues.put(OBJECT_STORAGE_URL_KEY, fullLocation);
        disasterRecoveryValues.put(RANGER_ADMIN_GROUP_KEY, rangerAdminGroup);
        disasterRecoveryValues.put(CLOSE_CONNECTIONS, String.valueOf(closeConnections));
        if (!skipDatabaseNames.isEmpty()) {
            List<String> names = DEFAULT_BACKUP_DATABASE.stream()
                    .map(DatabaseType::toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            skipDatabaseNames.stream()
                    .filter(((Predicate<String>) names::remove).negate())
                    .map(db -> "Tried to skip unknown database " + db)
                    .forEach(LOGGER::warn);
            disasterRecoveryValues.put(DATABASE_NAMES_KEY, String.join(" ", names));
        } else {
            disasterRecoveryValues.put(DATABASE_NAMES_KEY, "DEFAULT");
        }
        disasterRecoveryValues.put(COMPRESSION_LEVEL, enableCompression ? FAST_COMPRESSION_VALUE : SKIP_COMPRESSION_VALUE);
        disasterRecoveryValues.put(RAZ_ENABLED, razEnabled ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        servicePillar.put("disaster-recovery", new SaltPillarProperties(POSTGRESQL_DISASTER_RECOVERY_PILLAR_PATH,
                singletonMap(DISASTER_RECOVERY_KEY, disasterRecoveryValues)));

        return new SaltConfig(servicePillar);
    }

    public SaltConfig createSaltConfig(SaltConfig saltConfig, String tempBackupDir, String tempRestoreDir) {
        Map<String, String> pillarValues = new HashMap<>();
        pillarValues.put(TEMP_BACKUP_DIR, tempBackupDir);
        pillarValues.put(TEMP_RESTORE_DIR, tempRestoreDir);

        saltConfig.getServicePillarConfig().put("backup-restore-config", new SaltPillarProperties(BACKUP_RESTORE_CONFIG_PATH,
                singletonMap(BACKUP_RESTORE_CONFIG, pillarValues)));
        return saltConfig;
    }

    private String buildFullLocation(String location, String backupId, String cloudPlatform) throws URISyntaxException {
        URI uri = new URI(location);
        String suffix = '/' + backupId + DATABASE_BACKUP_POSTFIX;
        String fullLocation;
        if (!Strings.isNullOrEmpty(uri.getScheme()) &&
                uri.getScheme().equalsIgnoreCase("hdfs")) {
            fullLocation = uri.toString();
        } else if (AWS.equalsIgnoreCase(cloudPlatform)) {
            fullLocation = "s3a://" + uri.getSchemeSpecificPart().replaceAll("^/+", "");
        } else if (AZURE.equalsIgnoreCase(cloudPlatform)) {
            fullLocation = "abfs://" + uri.getSchemeSpecificPart().replaceAll("^/+", "");
        } else if (GCP.equalsIgnoreCase(cloudPlatform)) {
            fullLocation = "gs://" + uri.getSchemeSpecificPart().replaceAll("^/+", "");
        } else if (MOCK.equalsIgnoreCase(cloudPlatform)) {
            fullLocation = "mock://" + uri.getSchemeSpecificPart().replaceAll("^/+", "");
        } else {
            throw new UnsupportedOperationException("Cloud platform not supported for backup/restore: " + cloudPlatform);
        }
        return fullLocation + suffix;
    }
}
