package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.GenericPasswordSecretGenerator;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

public abstract class CMUserRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();

        Secret userSecret = getUserSecret(cluster);
        Secret passwordSecret = getPasswordSecret(cluster);
        Secret clientUserSecret = getClientUserSecret(cluster);
        Secret clientPasswordSecret = getClientPasswordSecret(cluster);
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withSecretGenerators(Map.of(
                        userSecret.getSecret(), CMUserNameSecretGenerator.class,
                        passwordSecret.getSecret(), GenericPasswordSecretGenerator.class
                ))
                .withSecretGeneratorArguments(Map.of(CMUserNameSecretGenerator.class,
                        Map.of(CMUserNameSecretGenerator.USER_PREFIX_MAP_KEY, getUserPrefix())))
                .build();
        CMUserRotationContext cmUserRotationContext = CMUserRotationContext.builder()
                .withUserSecret(userSecret.getSecret())
                .withPasswordSecret(passwordSecret.getSecret())
                .withClientUserSecret(clientUserSecret.getSecret())
                .withClientPasswordSecret(clientPasswordSecret.getSecret())
                .withResourceCrn(stack.getResourceCrn())
                .build();
        ClusterProxyRotationContext clusterProxyRotationContext = ClusterProxyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .build();
        result.put(SecretRotationStep.CLUSTER_PROXY, clusterProxyRotationContext);
        result.put(SecretRotationStep.VAULT, vaultRotationContext);
        result.put(SecretRotationStep.CM_USER, cmUserRotationContext);
        return result;
    }

    protected abstract String getUserPrefix();

    protected abstract Secret getUserSecret(ClusterView cluster);

    protected abstract Secret getPasswordSecret(ClusterView cluster);

    protected abstract Secret getClientUserSecret(ClusterView cluster);

    protected abstract Secret getClientPasswordSecret(ClusterView cluster);
}
