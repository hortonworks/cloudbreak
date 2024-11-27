package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component
public class DBStackStatusSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackStatusSyncService.class);

    private static final List<ExternalDatabaseStatus> UNACCEPTABLE_STATES =
            List.of(ExternalDatabaseStatus.DELETE_IN_PROGRESS, ExternalDatabaseStatus.DELETED, ExternalDatabaseStatus.UNKNOWN);

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

    @Inject
    private DBResourceService dbResourceService;

    @Inject
    private DBStackService dbStackService;

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
            case STARTED:
                return DetailedDBStackStatus.STARTED;
            case STOPPED:
                return DetailedDBStackStatus.STOPPED;
            case STOP_IN_PROGRESS:
                return DetailedDBStackStatus.STOP_IN_PROGRESS;
            case START_IN_PROGRESS:
                return DetailedDBStackStatus.START_IN_PROGRESS;
            case DELETE_IN_PROGRESS:
                return DetailedDBStackStatus.DELETE_IN_PROGRESS;
            case DELETED:
                return DetailedDBStackStatus.DELETE_COMPLETED;
            default:
                return DetailedDBStackStatus.UNKNOWN;
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

            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            DatabaseStack databaseStack = databaseStackConverter.convert(dbStack);

            Optional<ExternalDatabaseStatus> databaseStatusOptional = ofNullable(connector.resources().getDatabaseServerStatus(ac, databaseStack));
            Boolean externalDatabaseDeleted = databaseStatusOptional.map(ExternalDatabaseStatus.DELETED::equals).orElse(false);
            if (isAzureSingleServer(dbStack, databaseStack) && externalDatabaseDeleted) {
                return handleSingleServerAutoMigration(dbStack, databaseStatusOptional, databaseStack, connector, ac);
            }
            return databaseStatusOptional;
        } catch (Exception ex) {
            LOGGER.error(":::Auto sync::: External DB status lookup failed.", ex);
            return empty();
        }
    }

    private Optional<ExternalDatabaseStatus> handleSingleServerAutoMigration(DBStack dbStack, Optional<ExternalDatabaseStatus> databaseStatusOptional,
            DatabaseStack databaseStack, CloudConnector connector, AuthenticatedContext ac) throws Exception {
        LOGGER.debug(":::Auto sync::: External DB is Azure Single Server and deleted. "
                + "Checking by Flexible Server resource Id in case of auto-migration");
        Optional<DBResource> serverResourceOptional =
                dbResourceService.findByStackAndNameAndType(dbStack.getId(), dbStack.getDatabaseServer().getName(), ResourceType.AZURE_DATABASE);
        if (serverResourceOptional.isEmpty()) {
            LOGGER.debug(":::Auto sync::: Single server resource not found");
            return databaseStatusOptional;
        } else {
            DBResource dbResource = serverResourceOptional.get();
            String flexibleRef = dbResource.getResourceReference()
                    .replace(AzureDatabaseType.SINGLE_SERVER.referenceType(), AzureDatabaseType.FLEXIBLE_SERVER.referenceType());
            LOGGER.debug(":::Auto sync::: Looking for migrated flexible database by reference: {}", flexibleRef);

            DatabaseStack flexibleStack = convertDatabaseStackToFlexible(databaseStack);
            ExternalDatabaseStatus flexibleStatus = connector.resources().getDatabaseServerStatus(ac, flexibleStack);
            if (!UNACCEPTABLE_STATES.contains(flexibleStatus)) {
                LOGGER.debug(":::Auto sync::: Flexible server exists in status: {}, updating resource", flexibleStatus);
                updateResourceReference(dbResource, flexibleRef);
                updateDatabaseType(dbStack);

                return ofNullable(flexibleStatus);
            }
            return databaseStatusOptional;
        }
    }

    private void updateResourceReference(DBResource dbResource, String flexibleRef) {
        dbResource.setResourceReference(flexibleRef);
        dbResourceService.save(dbResource);
    }

    private void updateDatabaseType(DBStack dbStack) {
        com.sequenceiq.redbeams.domain.stack.DatabaseServer databaseServer = dbStack.getDatabaseServer();
        Json attributes = databaseServer.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name());
        databaseServer.setAttributes(new Json(params));
        dbStackService.save(dbStack);
    }

    private DatabaseStack convertDatabaseStackToFlexible(DatabaseStack databaseStack) {
        Map<String, Object> params = new HashMap<>(databaseStack.getDatabaseServer().getParameters());
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name());

        DatabaseServer flexibleDbServer = DatabaseServer.builder(databaseStack.getDatabaseServer())
                .withParams(params)
                .build();

        return new DatabaseStack(databaseStack.getNetwork(), flexibleDbServer, databaseStack.getTags(), databaseStack.getTemplate());
    }

    private boolean isAzureSingleServer(DBStack dbStack, DatabaseStack databaseStack) {
        String azureDatabaseType = databaseStack.getDatabaseServer().getStringParameter(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
        return AZURE.equalsIgnoreCase(dbStack.getCloudPlatform())
                && AzureDatabaseType.safeValueOf(azureDatabaseType) == AzureDatabaseType.SINGLE_SERVER;
    }
}
