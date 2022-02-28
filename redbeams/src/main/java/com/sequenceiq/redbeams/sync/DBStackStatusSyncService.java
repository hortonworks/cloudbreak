package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component
public class DBStackStatusSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackStatusSyncService.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private DBStackJobService dbStackJobService;

    public void sync(DBStack dbStack) {
        DetailedDBStackStatus detailedDBStackStatus = getDetailedDBStackStatusFromProvider(dbStack);
        Status status = detailedDBStackStatus.getStatus();

        if (dbStack.getStatus() != status) {
            if (status == null) {
                LOGGER.warn(":::Auto sync::: Can not update DBStack status because 'ExternalDatabaseStatus.{}' is mapped to 'null'",
                        detailedDBStackStatus);
            } else {
                LOGGER.debug(":::Auto sync::: Update DB Stack Status from '{}' to '{}'", dbStack.getStatus(), status);

                dbStackStatusUpdater.updateStatus(dbStack.getId(), detailedDBStackStatus);
            }
        }

        if (status != null && Status.getUnscheduleAutoSyncStatuses().contains(status)) {
            LOGGER.debug(":::Auto sync::: Unschedule DB Stack Status sync as the status is '{}'", status);
            dbStackJobService.unschedule(dbStack.getId(), dbStack.getName());
        }
    }

    private DetailedDBStackStatus getDetailedDBStackStatusFromProvider(DBStack dbStack) {
        Optional<ExternalDatabaseStatus> externalDatabaseStatus = getExternalDatabaseStatus(dbStack);
        DetailedDBStackStatus detailedDBStackStatus = externalDatabaseStatus
                .map(this::convert)
                .orElse(DetailedDBStackStatus.UNKNOWN);

        LOGGER.debug(":::Auto sync::: ExternalDatabaseStatus.{} got converted to DetailedDBStackStatus.{}",
                externalDatabaseStatus, detailedDBStackStatus);

        return detailedDBStackStatus;
    }

    private DetailedDBStackStatus convert(ExternalDatabaseStatus externalDatabaseStatus) {
        switch (externalDatabaseStatus) {
            case STARTED: return DetailedDBStackStatus.STARTED;
            case STOPPED: return DetailedDBStackStatus.STOPPED;
            case STOP_IN_PROGRESS: return DetailedDBStackStatus.STOP_IN_PROGRESS;
            case START_IN_PROGRESS: return DetailedDBStackStatus.START_IN_PROGRESS;
            case DELETE_IN_PROGRESS: return DetailedDBStackStatus.DELETE_IN_PROGRESS;
            case DELETED: return DetailedDBStackStatus.DELETE_COMPLETED;
            default: return DetailedDBStackStatus.UNKNOWN;
        }
    }

    private Optional<ExternalDatabaseStatus> getExternalDatabaseStatus(DBStack dbStack) {
        try {
            Location location = location(region(dbStack.getRegion()), availabilityZone(dbStack.getAvailabilityZone()));
            String accountId = dbStack.getOwnerCrn().getAccountId();
            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(dbStack.getId())
                    .withName(dbStack.getName())
                    .withCrn(dbStack.getResourceCrn())
                    .withPlatform(dbStack.getCloudPlatform())
                    .withVariant(dbStack.getPlatformVariant())
                    .withLocation(location)
                    .withUserName(dbStack.getUserName())
                    .withAccountId(accountId)
                    .build();
            Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
            CloudCredential cloudCredential = credentialConverter.convert(credential);

            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            DatabaseStack databaseStack = databaseStackConverter.convert(dbStack);

            return ofNullable(connector.resources().getDatabaseServerStatus(ac, databaseStack));
        } catch (Exception ex) {
            LOGGER.error(":::Auto sync::: External DB status lookup failed.", ex);
            return empty();
        }
    }
}
