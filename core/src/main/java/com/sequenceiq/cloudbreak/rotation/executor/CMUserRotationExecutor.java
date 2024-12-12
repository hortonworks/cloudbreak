package com.sequenceiq.cloudbreak.rotation.executor;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMUserRotationExecutor extends AbstractRotationExecutor<CMUserRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMUserRotationExecutor.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Override
    protected void rotate(CMUserRotationContext rotationContext) throws Exception {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        RotationSecret clientUser = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientUserSecret());
        RotationSecret clientPassword = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientPasswordSecret());
        rotationContext.getRotatableSecrets().forEach(secretPair -> {
            try {
                RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(secretPair.getKey());
                RotationSecret passwordRotationSecret = uncachedSecretServiceForRotation.getRotation(secretPair.getValue());
                if (userRotationSecret.isRotation() && passwordRotationSecret.isRotation()) {
                    LOGGER.info("Starting rotation of CM user by creating a new user {} using {} as API client user.",
                            userRotationSecret.getSecret(), clientUser.getBackupSecret());
                    clusterSecurityService.createNewUser(
                            userRotationSecret.getBackupSecret(),
                            userRotationSecret.getSecret(),
                            passwordRotationSecret.getSecret(),
                            clientUser.getBackupSecret(),
                            clientPassword.getBackupSecret());
                } else {
                    throw new SecretRotationException("User or password is not in rotation state in Vault, thus rotation of CM user is not possible.");
                }
            } catch (CloudbreakException e) {
                throw new SecretRotationException(e);
            }
        });
    }

    @Override
    protected void rollback(CMUserRotationContext rotationContext) throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        RotationSecret clientUser = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientUserSecret());
        RotationSecret clientPassword = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientPasswordSecret());
        rotationContext.getRotatableSecrets().forEach(secretPair -> {
            try {
                RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(secretPair.getKey());
                if (userRotationSecret.isRotation()) {
                    LOGGER.info("Starting to rollback rotation of CM user by deleting the new user {} using {} as API client user.",
                            userRotationSecret.getSecret(), clientUser.getBackupSecret());
                    clusterSecurityService.deleteUser(
                            userRotationSecret.getSecret(),
                            clientUser.getBackupSecret(),
                            clientPassword.getBackupSecret());
                } else {
                    throw new SecretRotationException("User is not in rotation state in Vault, thus we cannot rollback it.");
                }
            } catch (CloudbreakException e) {
                throw new SecretRotationException(e);
            }
        });
    }

    @Override
    protected void finalize(CMUserRotationContext rotationContext) throws Exception {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        RotationSecret clientUser = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientUserSecret());
        RotationSecret clientPassword = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientPasswordSecret());
        rotationContext.getRotatableSecrets().forEach(secretPair -> {
            try {
                RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(secretPair.getKey());
                if (userRotationSecret.isRotation()) {
                    LOGGER.info("Finalizing rotation of CM user by deleting the old user {} using {} as API client user.",
                            userRotationSecret.getBackupSecret(), clientUser.getSecret());
                    clusterSecurityService.deleteUser(
                            userRotationSecret.getBackupSecret(),
                            clientUser.getSecret(),
                            clientPassword.getSecret());
                } else {
                    throw new SecretRotationException("User is not in rotation state in Vault, thus we cannot finalize it.");
                }
            } catch (CloudbreakException e) {
                throw new SecretRotationException(e);
            }
        });
    }

    @Override
    protected void preValidate(CMUserRotationContext rotationContext) throws Exception {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        String clientUser = uncachedSecretServiceForRotation.get(rotationContext.getClientUserSecret());
        String clientPassword = uncachedSecretServiceForRotation.get(rotationContext.getClientPasswordSecret());
        rotationContext.getRotatableSecrets().forEach(secretPair -> {
            try {
                String userSecret = uncachedSecretServiceForRotation.get(secretPair.getKey());
                String passwordRotationSecret = uncachedSecretServiceForRotation.get(secretPair.getValue());
                LOGGER.info("Checking if user {} is present in CM!", userSecret);
                clusterSecurityService.checkUser(userSecret, clientUser, clientPassword);
                LOGGER.info("Checking if CM API call is possible with user {}!", userSecret);
                getClusterSecurityService(rotationContext).testUser(userSecret, passwordRotationSecret);
            } catch (Exception e) {
                throw new SecretRotationException(e);
            }
        });
    }

    @Override
    protected void postValidate(CMUserRotationContext rotationContext) throws Exception {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        RotationSecret clientUser = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientUserSecret());
        RotationSecret clientPassword = uncachedSecretServiceForRotation.getRotation(rotationContext.getClientPasswordSecret());
        rotationContext.getRotatableSecrets().forEach(secretPair -> {
            try {
                RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(secretPair.getKey());
                RotationSecret passwordRotationSecret = uncachedSecretServiceForRotation.getRotation(secretPair.getValue());
                if (userRotationSecret.isRotation() && passwordRotationSecret.isRotation()) {
                    LOGGER.info("Checking if new user {} is present in CM!", userRotationSecret.getSecret());
                    clusterSecurityService.checkUser(userRotationSecret.getSecret(), clientUser.getSecret(), clientPassword.getSecret());
                    LOGGER.info("Checking if CM API call is possible with new user {}!", userRotationSecret.getSecret());
                    getClusterSecurityService(rotationContext).testUser(userRotationSecret.getSecret(), passwordRotationSecret.getSecret());
                } else {
                    throw new SecretRotationException("User and password is not in rotation state in Vault, thus rotation of CM user have been failed.");
                }
            } catch (Exception e) {
                throw new SecretRotationException(e);
            }
        });
    }

    private ClusterSecurityService getClusterSecurityService(RotationContext rotationContext) {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        return clusterApiConnectors.getConnector(stack).clusterSecurityService();
    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.CM_USER;
    }

    @Override
    protected Class<CMUserRotationContext> getContextClass() {
        return CMUserRotationContext.class;
    }
}
