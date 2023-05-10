package com.sequenceiq.cloudbreak.rotation.context.provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class MgmtCMAdminPasswordRotationContextProvider extends CMUserRotationContextProvider {

    private static final String USER_PREFIX = "mgmt";

    @Override
    protected String getUserPrefix() {
        return USER_PREFIX;
    }

    @Override
    protected Secret getUserSecret(ClusterView cluster) {
        return cluster.getDpClusterManagerUserSecret();
    }

    @Override
    protected Secret getPasswordSecret(ClusterView cluster) {
        return cluster.getDpClusterManagerPasswordSecret();
    }

    @Override
    protected Secret getClientUserSecret(ClusterView cluster) {
        return cluster.getCloudbreakAmbariUserSecret();
    }

    @Override
    protected Secret getClientPasswordSecret(ClusterView cluster) {
        return cluster.getCloudbreakAmbariPasswordSecret();
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.MGMT_CM_ADMIN_PASSWORD;
    }
}
