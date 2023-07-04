package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyRotationContext;
import com.sequenceiq.cloudbreak.rotation.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

public abstract class CMUserRotationContextProvider implements RotationContextProvider {

    private static final String DATETIMEFORMAT = "ddMMyyHHmmss";

    @Inject
    private StackDtoService stackService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();

        Secret userSecret = getUserSecret(cluster);
        Secret passwordSecret = getPasswordSecret(cluster);

        String newUser = getUserPrefix() + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());
        String newPassword = PasswordUtil.generatePassword();

        Map<String, String> vaultPathMap = Maps.newHashMap();
        vaultPathMap.putAll(getDuplicatedUserSecrets(cluster).stream().collect(Collectors.toMap(Secret::getSecret, secret -> newUser)));
        vaultPathMap.putAll(getDuplicatedPasswordSecrets(cluster).stream().collect(Collectors.toMap(Secret::getSecret, secret -> newPassword)));
        vaultPathMap.put(userSecret.getSecret(), newUser);
        vaultPathMap.put(passwordSecret.getSecret(), newPassword);
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withVaultPathSecretMap(vaultPathMap)
                .build();

        CMUserRotationContext cmUserRotationContext = CMUserRotationContext.builder()
                .withUserSecret(getUserSecret(cluster).getSecret())
                .withPasswordSecret(getPasswordSecret(cluster).getSecret())
                .withClientUserSecret(getClientUserSecret(cluster).getSecret())
                .withClientPasswordSecret(getClientPasswordSecret(cluster).getSecret())
                .withResourceCrn(stack.getResourceCrn())
                .build();

        ClusterProxyRotationContext clusterProxyRotationContext = ClusterProxyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .build();

        result.put(CloudbreakSecretRotationStep.CLUSTER_PROXY, clusterProxyRotationContext);
        result.put(CommonSecretRotationStep.VAULT, vaultRotationContext);
        result.put(CloudbreakSecretRotationStep.CM_USER, cmUserRotationContext);
        return result;
    }

    protected abstract String getUserPrefix();

    protected abstract Secret getClientUserSecret(ClusterView cluster);

    protected abstract Secret getClientPasswordSecret(ClusterView cluster);

    protected abstract Secret getUserSecret(ClusterView cluster);

    protected abstract Secret getPasswordSecret(ClusterView cluster);

    // created different getters for duplicated secrets intentionally
    // should be removed if duplicated secrets are also removed from entity (Cluster typically)
    // these should be used only to update duplicated vault paths, every other use case should ignore/not use these getters but the ones above
    protected abstract Set<Secret> getDuplicatedUserSecrets(ClusterView cluster);

    protected abstract Set<Secret> getDuplicatedPasswordSecrets(ClusterView cluster);
}
