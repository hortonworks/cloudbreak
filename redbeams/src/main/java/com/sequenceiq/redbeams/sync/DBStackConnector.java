package com.sequenceiq.redbeams.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;

/**
 * Builds the authenticated cloud connector context needed to talk to the provider about a {@link DBStack}'s database server.
 */
@Component
public class DBStackConnector {

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    public ConnectedDatabaseStack connect(DBStack dbStack) {
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
        return new ConnectedDatabaseStack(connector, ac, databaseStack);
    }

    public record ConnectedDatabaseStack(CloudConnector connector, AuthenticatedContext authenticatedContext, DatabaseStack databaseStack) {
    }
}
