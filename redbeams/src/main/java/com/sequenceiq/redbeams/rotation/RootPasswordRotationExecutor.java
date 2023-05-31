package com.sequenceiq.redbeams.rotation;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD;

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
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
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
public class RootPasswordRotationExecutor implements RotationExecutor<RotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootPasswordRotationExecutor.class);

    private static final String ROTATION_STATE = "rotation";

    private static final String ROLLBACK_STATE = "rollback";

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
    public void rotate(RotationContext rotationContext) {
        try {
            updateRootPasswordOnProvider(rotationContext, false);
        } catch (SecretRotationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Rotation of {} failed for {}", getType(), rotationContext.getResourceCrn(), e);
            throw new SecretRotationException(e, getType());
        }
    }

    @Override
    public void rollback(RotationContext rotationContext) {
        try {
            updateRootPasswordOnProvider(rotationContext, true);
        } catch (SecretRotationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Rollback of {} failed for {}", getType(), rotationContext.getResourceCrn(), e);
            throw new SecretRotationException(e, getType());
        }
    }

    @Override
    public void finalize(RotationContext rotationContext) {
        LOGGER.info("Database root password finalize finished, nothing to do.");
    }

    @Override
    public SecretRotationStep getType() {
        return PROVIDER_DATABASE_ROOT_PASSWORD;
    }

    @Override
    public Class<RotationContext> getContextClass() {
        return RotationContext.class;
    }

    private void updateRootPasswordOnProvider(RotationContext rotationContext, boolean rollback) {
        String rotationState = getRotationState(rollback);
        LOGGER.info("Starting {} of database root password: {}", rotationState, rotationContext.getResourceCrn());
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
        } else {
            throw new SecretRotationException("Root password is not in rotation state in Vault, thus " + rotationState + " is not possible.", getType());
        }
        LOGGER.info("Database root password {} finished: {}", rotationState, rotationContext.getResourceCrn());
    }

    private String getRotationState(boolean rollback) {
        return rollback ? ROLLBACK_STATE : ROTATION_STATE;
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
