package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Component
public class EmbeddedDatabaseConfigProvider {
    public static final String POSTGRES_DIRECTORY_KEY = "postgres_directory";

    public static final String POSTGRES_LOG_DIRECTORY_KEY = "postgres_log_directory";

    public static final String POSTGRES_SCRIPTS_EXECUTED_DIRECTORY_KEY = "postgres_scripts_executed_directory";

    public static final String POSTGRES_DATA_ON_ATTACHED_DISK_KEY = "postgres_data_on_attached_disk";

    public static final String POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK = "pgsql";

    public static final String POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK = "pgsql/log";

    public static final String POSTGRES_SCRIPTS_EXECUTED_SUBDIRECTORY_ON_ATTACHED_DISK = "pgsql/scripts";

    public static final String POSTGRES_DEFAULT_DIRECTORY = "/var/lib/pgsql";

    public static final String POSTGRES_DEFAULT_LOG_DIRECTORY = "/var/log";

    public static final String POSTGRES_DEFAULT_SCRIPTS_EXECUTED_DIRECTORY = "/opt/salt/scripts";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDatabaseConfigProvider.class);

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    public Map<String, Object> collectEmbeddedDatabaseConfigs(Stack stack) {
        Map<String, Object> result;
        if (embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack)) {
            LOGGER.info("Attached disk will be used to store data for postgres sql server");
            result = createEmbeddedDbOnAttachedDiskConfig();
        } else {
            LOGGER.info("Default settings for data storage will be used for postgres sql server as no disks are attached");
            result = Map.of(
                    POSTGRES_DIRECTORY_KEY, POSTGRES_DEFAULT_DIRECTORY,
                    POSTGRES_LOG_DIRECTORY_KEY, POSTGRES_DEFAULT_LOG_DIRECTORY,
                    POSTGRES_SCRIPTS_EXECUTED_DIRECTORY_KEY, POSTGRES_DEFAULT_SCRIPTS_EXECUTED_DIRECTORY,
                    POSTGRES_DATA_ON_ATTACHED_DISK_KEY, false);
        }
        LOGGER.debug("Embedded Postgres sql server pillar parameters: {}", result);
        return result;
    }

    private Map<String, Object> createEmbeddedDbOnAttachedDiskConfig() {
        return Map.of(
                POSTGRES_DIRECTORY_KEY, VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK,
                POSTGRES_LOG_DIRECTORY_KEY, VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK,
                POSTGRES_SCRIPTS_EXECUTED_DIRECTORY_KEY, VolumeUtils.DATABASE_VOLUME + "/" + POSTGRES_SCRIPTS_EXECUTED_SUBDIRECTORY_ON_ATTACHED_DISK,
                POSTGRES_DATA_ON_ATTACHED_DISK_KEY, true);
    }
}
