package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class CBCMAdminPasswordRotationContextProvider extends CMUserRotationContextProvider {

    private static final String USER_PREFIX = "cb";

    @Override
    protected String getUserPrefix() {
        return USER_PREFIX;
    }

    @Override
    protected Secret getUserSecret(ClusterView cluster) {
        return cluster.getCloudbreakClusterManagerUserSecret();
    }

    @Override
    protected Secret getPasswordSecret(ClusterView cluster) {
        return cluster.getCloudbreakClusterManagerPasswordSecret();
    }

    @Override
    protected Secret getClientUserSecret(ClusterView cluster) {
        return cluster.getClusterManagerMgmtUserSecret();
    }

    @Override
    protected Secret getClientPasswordSecret(ClusterView cluster) {
        return cluster.getClusterManagerMgmtPasswordSecret();
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
    }
}
