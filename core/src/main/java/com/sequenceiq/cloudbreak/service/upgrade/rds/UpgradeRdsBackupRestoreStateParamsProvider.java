package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static java.util.Collections.singletonMap;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Component
public class UpgradeRdsBackupRestoreStateParamsProvider {
    private static final String POSTGRESQL_UPGRADE = "postgresql-upgrade";

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    public Map<String, SaltPillarProperties> createParamsForRdsBackupRestore(StackDto stackDto, String rdsBackupLocation) {
        String backupLocation = determineRdsBackupLocation(stackDto, rdsBackupLocation);
        Map<String, String> backupProperties = Map.of(
                "compressed_file_name", "rds_backup.tar.gz",
                "directory", backupLocation + "/tmp/postgres_upgrade_backup",
                "logfile", "/var/log/postgres_upgrade_backup.log");
        Map<String, String> restoreProperties = Map.of(
                "logfile", "/var/log/postgres_upgrade_restore.log");
        Map<String, String> checkConnectionProperties = Map.of(
                "logfile", "/var/log/postgres_upgrade_checkconnection.log");
        return singletonMap(POSTGRESQL_UPGRADE, new SaltPillarProperties("/postgresql/upgrade.sls",
                singletonMap("upgrade", Map.of(
                        "backup", backupProperties,
                        "restore", restoreProperties,
                        "checkconnection", checkConnectionProperties))));
    }

    private String determineRdsBackupLocation(StackDto stackDto, String rdsBackupLocation) {
        String backupLocation;
        if (RedbeamsDbServerConfigurer.isRemoteDatabaseRequested(stackDto.getCluster().getDatabaseServerCrn())
                || !embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto)) {
            backupLocation = StringUtils.isNotEmpty(rdsBackupLocation) ? rdsBackupLocation : "/var";
        } else {
            backupLocation = VolumeUtils.DATABASE_VOLUME;
        }
        return backupLocation;
    }
}
