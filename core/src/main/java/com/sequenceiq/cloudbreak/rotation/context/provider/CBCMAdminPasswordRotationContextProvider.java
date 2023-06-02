package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;
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
    protected Set<Secret> getDuplicatedUserSecrets(ClusterView cluster) {
        return Set.of(cluster.getCloudbreakAmbariUserSecret());
    }

    @Override
    protected Set<Secret> getDuplicatedPasswordSecrets(ClusterView cluster) {
        return Set.of(cluster.getCloudbreakAmbariPasswordSecret());
    }

    @Override
    protected Secret getUserSecret(ClusterView cluster) {
        return cluster.getCloudbreakClusterManagerUserSecretObject();
    }

    @Override
    protected Secret getPasswordSecret(ClusterView cluster) {
        return cluster.getCloudbreakClusterManagerPasswordSecretObject();
    }

    @Override
    protected Secret getClientUserSecret(ClusterView cluster) {
        return cluster.getDpClusterManagerUserSecret();
    }

    @Override
    protected Secret getClientPasswordSecret(ClusterView cluster) {
        return cluster.getDpClusterManagerPasswordSecret();
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
    }
}
