package com.sequenceiq.cloudbreak.rotation.executor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMUserRotationExecutor implements RotationExecutor<CMUserRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMUserRotationExecutor.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackService;

    @Inject
    private SecretService secretService;

    @Override
    public void rotate(CMUserRotationContext rotationContext) {
        LOGGER.info("Starting rotation of CM user for resource {} by creating a new user.", rotationContext.getResourceCrn());
        try {
            RotationSecret userRotationSecret = secretService.getRotation(rotationContext.getUserSecret());
            RotationSecret passwordRotationSecret = secretService.getRotation(rotationContext.getPasswordSecret());
            if (userRotationSecret.isRotation() && passwordRotationSecret.isRotation()) {
                ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
                String clientUser = secretService.get(rotationContext.getClientUserSecret());
                String clientPassword = secretService.get(rotationContext.getClientPasswordSecret());
                clusterSecurityService.createNewUser(
                        userRotationSecret.getBackupSecret(),
                        userRotationSecret.getSecret(),
                        passwordRotationSecret.getSecret(),
                        clientUser,
                        clientPassword);
            } else {
                throw new SecretRotationException("User or password is not in rotation state in Vault, thus rotation of CM user is not possible.", getType());
            }
        } catch (Exception e) {
            LOGGER.error("Rotation of CM user failed, ", e);
            throw new SecretRotationException(e, getType());
        }
    }

    @Override
    public void rollback(CMUserRotationContext rotationContext) {
        LOGGER.info("Starting to rollback rotation of CM user for resource {} by deleting the new user", rotationContext.getResourceCrn());
        RotationSecret userRotationSecret = secretService.getRotation(rotationContext.getUserSecret());
        if (userRotationSecret.isRotation()) {
            deleteUser(userRotationSecret.getSecret(), rotationContext);
        } else {
            throw new SecretRotationException("User is not in rotation state in Vault, thus we cannot rollback it.", getType());
        }
    }

    @Override
    public void finalize(CMUserRotationContext rotationContext) {
        LOGGER.info("Finalizing rotation of CM user for resource {} by deleting the old user", rotationContext.getResourceCrn());
        RotationSecret userRotationSecret = secretService.getRotation(rotationContext.getUserSecret());
        if (userRotationSecret.isRotation()) {
            deleteUser(userRotationSecret.getBackupSecret(), rotationContext);
        } else {
            throw new SecretRotationException("User is not in rotation state in Vault, thus we cannot finalize it.", getType());
        }
    }

    private void deleteUser(String user, CMUserRotationContext rotationContext) {
        ClusterSecurityService clusterSecurityService = getClusterSecurityService(rotationContext);
        String clientUser = secretService.get(rotationContext.getClientUserSecret());
        String clientPassword = secretService.get(rotationContext.getClientPasswordSecret());
        try {
            clusterSecurityService.deleteUser(
                    user,
                    clientUser,
                    clientPassword);
        } catch (CloudbreakException e) {
            throw new SecretRotationException(e, getType());
        }
    }

    private ClusterSecurityService getClusterSecurityService(RotationContext rotationContext) {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        ClusterSecurityService clusterSecurityService = clusterApiConnectors.getConnector(stack).clusterSecurityService();
        return clusterSecurityService;
    }

    @Override
    public SecretRotationStep getType() {
        return SecretRotationStep.CM_USER;
    }
}
