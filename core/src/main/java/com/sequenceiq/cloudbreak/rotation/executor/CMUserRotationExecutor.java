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
        LOGGER.info("Starting rotation of CM user by creating a new user.");
        RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getUserSecret());
        RotationSecret passwordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getPasswordSecret());
        if (userRotationSecret.isRotation() && passwordRotationSecret.isRotation()) {
            ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
            String clientUser = uncachedSecretServiceForRotation.get(rotationContext.getClientUserSecret());
            String clientPassword = uncachedSecretServiceForRotation.get(rotationContext.getClientPasswordSecret());
            clusterSecurityService.createNewUser(
                    userRotationSecret.getBackupSecret(),
                    userRotationSecret.getSecret(),
                    passwordRotationSecret.getSecret(),
                    clientUser,
                    clientPassword);
        } else {
            throw new SecretRotationException("User or password is not in rotation state in Vault, thus rotation of CM user is not possible.");
        }
    }

    @Override
    protected void rollback(CMUserRotationContext rotationContext) throws CloudbreakException {
        LOGGER.info("Starting to rollback rotation of CM user by deleting the new user");
        RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getUserSecret());
        if (userRotationSecret.isRotation()) {
            deleteUser(userRotationSecret.getSecret(), rotationContext);
        } else {
            throw new SecretRotationException("User is not in rotation state in Vault, thus we cannot rollback it.");
        }
    }

    @Override
    protected void finalize(CMUserRotationContext rotationContext) throws Exception {
        LOGGER.info("Finalizing rotation of CM user by deleting the old user");
        RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getUserSecret());
        if (userRotationSecret.isRotation()) {
            deleteUser(userRotationSecret.getBackupSecret(), rotationContext);
        } else {
            throw new SecretRotationException("User or password is not in rotation state in Vault, thus we cannot finalize it.");
        }
    }

    @Override
    protected void preValidate(CMUserRotationContext rotationContext) throws Exception {
        String user = uncachedSecretServiceForRotation.get(rotationContext.getUserSecret());
        checkUser(rotationContext, user);
    }

    @Override
    protected void postValidate(CMUserRotationContext rotationContext) throws Exception {
        RotationSecret userRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getUserSecret());
        RotationSecret passwordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getPasswordSecret());
        if (userRotationSecret.isRotation() && passwordRotationSecret.isRotation()) {
            LOGGER.info("Checking if new user is present in CM!");
            checkUser(rotationContext, userRotationSecret.getSecret());
            LOGGER.info("Checking if CM API call is possible with new user!");
            getClusterSecurityService(rotationContext).testUser(userRotationSecret.getSecret(), passwordRotationSecret.getSecret());
        } else {
            throw new SecretRotationException("User and password is not in rotation state in Vault, thus rotation of CM user have been failed.");
        }
    }

    private void checkUser(CMUserRotationContext rotationContext, String user) throws Exception {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        String clientUser = uncachedSecretServiceForRotation.get(rotationContext.getClientUserSecret());
        String clientPassword = uncachedSecretServiceForRotation.get(rotationContext.getClientPasswordSecret());
        clusterSecurityService.checkUser(user, clientUser, clientPassword);
    }

    private void deleteUser(String user, CMUserRotationContext rotationContext) throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        String clientUser = uncachedSecretServiceForRotation.get(rotationContext.getClientUserSecret());
        String clientPassword = uncachedSecretServiceForRotation.get(rotationContext.getClientPasswordSecret());
        clusterSecurityService.deleteUser(
                user,
                clientUser,
                clientPassword);
    }

    private ClusterSecurityService getClusterSecurityService(RotationContext rotationContext) {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        ClusterSecurityService clusterSecurityService = clusterApiConnectors.getConnector(stack).clusterSecurityService();
        return clusterSecurityService;
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
