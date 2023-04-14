package com.sequenceiq.cloudbreak.rotation.cm;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMUserRotationExecutor implements RotationExecutor {

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackService;

    @Inject
    private SecretService secretService;

    @Override
    public void rotate(RotationContext rotationContext) {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        ClusterSecurityService clusterSecurityService = clusterApiConnectors.getConnector(stack).clusterSecurityService();
        String clientUser = secretService.get(rotationContext.getClientUserSecretSupplier().get());
        String clientPassword = secretService.get(rotationContext.getClientPasswordSecretSupplier().get());
        rotationContext.getUserPasswordSecrets().forEach((userSecret, passwordSecret) -> {
            try {
                // TODO we should use different username
                String user = secretService.get(userSecret);
                String newPassword = secretService.get(passwordSecret);
                clusterSecurityService.updateExistingUser(clientUser, clientPassword, user, newPassword);
            } catch (CloudbreakException e) {
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void rollback(RotationContext rotationContext) {
        // TODO remove new user if already created, otherwise nothing to do
    }

    @Override
    public void finalize(RotationContext rotationContext) {
        // TODO cleanup old user
    }

    @Override
    public SecretLocationType getType() {
        return SecretLocationType.CM_USER;
    }
}
