package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyReRegisterRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class CBCMAdminPasswordRotationContextProvider implements RotationContextProvider {

    private static final String DATETIMEFORMAT = "ddMMyyHHmmss";

    private static final String CB_USER_PREFIX = "cb";

    private static final String MGMT_USER_PREFIX = "mgmt";

    @Inject
    private StackDtoService stackService;

    @Inject
    private ClusterService clusterService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        Cluster cluster = clusterService.getClusterByStackResourceCrn(resourceCrn);

        String newCbUser = CB_USER_PREFIX + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());
        String newCbPassword = PasswordUtil.generatePassword();

        String newMgmtUser = MGMT_USER_PREFIX + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());
        String newMgmtPassword = PasswordUtil.generatePassword();

        Map<String, String> newSecretMap = Maps.newHashMap();
        newSecretMap.put(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret(), newCbUser);
        newSecretMap.put(cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret(), newCbPassword);
        newSecretMap.put(cluster.getDpClusterManagerUserSecret().getSecret(), newMgmtUser);
        newSecretMap.put(cluster.getDpClusterManagerPasswordSecret().getSecret(), newMgmtPassword);

        Map<String, Consumer<String>> secretUpdaterMap = Maps.newHashMap();
        secretUpdaterMap.put(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret(),
                vaultSecretJson -> cluster.setCloudbreakClusterManagerUserSecret(new SecretProxy(vaultSecretJson)));
        secretUpdaterMap.put(cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret(),
                vaultSecretJson -> cluster.setCloudbreakClusterManagerPasswordSecret(new SecretProxy(vaultSecretJson)));
        secretUpdaterMap.put(cluster.getDpClusterManagerUserSecret().getSecret(),
                vaultSecretJson -> cluster.setDpClusterManagerUserSecret(new SecretProxy(vaultSecretJson)));
        secretUpdaterMap.put(cluster.getDpClusterManagerPasswordSecret().getSecret(),
                vaultSecretJson -> cluster.setDpClusterManagerPasswordSecret(new SecretProxy(vaultSecretJson)));

        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withNewSecretMap(newSecretMap)
                .withEntitySecretFieldUpdaterMap(secretUpdaterMap)
                .withEntitySaverList(List.of(() -> clusterService.save(cluster)))
                .build();

        CMUserRotationContext cmUserRotationContext = CMUserRotationContext.builder()
                .withRotatableSecrets(Set.of(
                        Pair.of(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret(),
                                cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret()),
                        Pair.of(cluster.getDpClusterManagerUserSecret().getSecret(),
                                cluster.getDpClusterManagerPasswordSecret().getSecret())))
                .withClientUserSecret(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret())
                .withClientPasswordSecret(cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret())
                .withResourceCrn(resourceCrn)
                .build();

        ClusterProxyReRegisterRotationContext clusterProxyReRegisterRotationContext = ClusterProxyReRegisterRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .build();

        result.put(CloudbreakSecretRotationStep.CLUSTER_PROXY_REREGISTER, clusterProxyReRegisterRotationContext);
        result.put(CommonSecretRotationStep.VAULT, vaultRotationContext);
        result.put(CloudbreakSecretRotationStep.CM_USER, cmUserRotationContext);
        return result;
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.CM_ADMIN_PASSWORD;
    }
}
