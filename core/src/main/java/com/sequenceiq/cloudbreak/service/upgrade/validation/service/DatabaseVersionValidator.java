package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Database;

@Component
public class DatabaseVersionValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVersionValidator.class);

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        String targetRuntime = validationRequest.upgradeImageInfo().getTargetStatedImage().getImage().getVersion();
        Database database = validationRequest.stack().getDatabase();
        String databaseVersion = database.getExternalDatabaseEngineVersion();
        if (isExternalDatabase(validationRequest) && isVersionNewerOrEqualThan731(targetRuntime) && isPostgresVersionOlderThanRequired(databaseVersion)) {
            String message = "To upgrade your cluster to version 7.3.1 or higher, your PostgreSQL version must be at least 14. "
                    + "Please update your database to version 14 or above and attempt the upgrade again.";
            LOGGER.error("Cluster upgrade validation failed. {} Target runtime {}, current database version {}", message, targetRuntime, databaseVersion);
            throw new UpgradeValidationFailedException(message);
        }
    }

    private boolean isExternalDatabase(ServiceUpgradeValidationRequest validationRequest) {
        DatabaseAvailabilityType databaseAvailabilityType = getDatabaseAvailabilityType(validationRequest);
        return !databaseAvailabilityType.isEmbedded();
    }

    private DatabaseAvailabilityType getDatabaseAvailabilityType(ServiceUpgradeValidationRequest validationRequest) {
        Database database = validationRequest.stack().getDatabase();
        StackType stackType = validationRequest.stack().getType();
        return StackType.DATALAKE.equals(stackType) ? getDatalakeDatabaseAvailabilityType(database) : database.getExternalDatabaseAvailabilityType();
    }

    private DatabaseAvailabilityType getDatalakeDatabaseAvailabilityType(Database database) {
        return Optional.ofNullable(database.getDatalakeDatabaseAvailabilityType())
                .orElse(DatabaseAvailabilityType.NONE);
    }

    private boolean isPostgresVersionOlderThanRequired(String databaseVersion) {
        return isVersionOlderThanLimited(databaseVersion, TargetMajorVersion.VERSION14::getMajorVersion);
    }

    private boolean isVersionNewerOrEqualThan731(String targetRuntimeVersion) {
        return isVersionNewerOrEqualThanLimited(targetRuntimeVersion, CLOUDERA_STACK_VERSION_7_3_1);
    }

}
