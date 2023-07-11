package com.sequenceiq.redbeams.rotation;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.redbeams.rotation.RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD;

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
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RootPasswordRotationExecutor extends AbstractRotationExecutor<RotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootPasswordRotationExecutor.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private DBStackToDatabaseStackConverter dbStackToDatabaseStackConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SecretService secretService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Override
    protected void rotate(RotationContext rotationContext) {
        LOGGER.info("Rotate database root password.");
        updateRootPasswordOnProvider(rotationContext, false);
    }

    @Override
    protected void rollback(RotationContext rotationContext) {
        LOGGER.info("Rollback database root password.");
        updateRootPasswordOnProvider(rotationContext, true);
    }

    @Override
    protected void finalize(RotationContext rotationContext) {
        LOGGER.info("Finalize database root password rotation, nothing to do.");
    }

    @Override
    protected void preValidate(RotationContext rotationContext) throws Exception {
        ExternalDatabaseStatus databaseServerStatus = getExternalDatabaseStatus(rotationContext);
        if (!ExternalDatabaseStatus.STARTED.equals(databaseServerStatus)) {
            throw new SecretRotationException("Database is not running or transition is in progress, thus rotation is not possible!", getType());
        }
    }

    @Override
    protected void postValidate(RotationContext rotationContext) throws Exception {
        ExternalDatabaseStatus databaseServerStatus = getExternalDatabaseStatus(rotationContext);
        if (!ExternalDatabaseStatus.STARTED.equals(databaseServerStatus)) {
            throw new SecretRotationException("Database is not running or transition is in progress, thus rotation has been failed!", getType());
        }
    }

    private ExternalDatabaseStatus getExternalDatabaseStatus(RotationContext rotationContext) throws Exception {
        DBStack dbStack = dbStackService.getByCrn(rotationContext.getResourceCrn());
        CloudContext cloudContext = getCloudContext(dbStack);
        Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        DatabaseStack databaseStack = dbStackToDatabaseStackConverter.convert(dbStack);
        return connector.resources().getDatabaseServerStatus(ac, databaseStack);
    }

    @Override
    public SecretRotationStep getType() {
        return PROVIDER_DATABASE_ROOT_PASSWORD;
    }

    @Override
    protected Class<RotationContext> getContextClass() {
        return RotationContext.class;
    }

    private void updateRootPasswordOnProvider(RotationContext rotationContext, boolean rollback) {
        DBStack dbStack = dbStackService.getByCrn(rotationContext.getResourceCrn());
        DatabaseServerConfig databaseServerConfig = databaseServerConfigService.getByCrn(rotationContext.getResourceCrn());
        RotationSecret databaseServerRootPasswordRotation = secretService.getRotation(dbStack.getDatabaseServer().getRootPasswordSecret());
        RotationSecret databaseServerConfigRootPasswordRotation = secretService.getRotation(databaseServerConfig.getConnectionPasswordSecret());
        if (databaseServerRootPasswordRotation.isRotation() && databaseServerConfigRootPasswordRotation.isRotation()) {
            CloudContext cloudContext = getCloudContext(dbStack);
            Credential credential = credentialService.getCredentialByEnvCrn(dbStack.getEnvironmentId());
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            DatabaseStack databaseStack = dbStackToDatabaseStackConverter.convert(dbStack);
            String password = rollback ? databaseServerRootPasswordRotation.getBackupSecret() : databaseServerRootPasswordRotation.getSecret();
            connector.resources().updateDatabaseRootPassword(ac, databaseStack, password);
        } else if (rollback) {
            LOGGER.warn("Root password is not in rotation state in Vault, rollback is not possible, return without errors.");
        } else {
            String message = "Root password is not in rotation state in Vault, rotation is not possible.";
            LOGGER.warn(message);
            throw new SecretRotationException(message, getType());
        }
    }

    private static CloudContext getCloudContext(DBStack dbStack) {
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
        return cloudContext;
    }
}
