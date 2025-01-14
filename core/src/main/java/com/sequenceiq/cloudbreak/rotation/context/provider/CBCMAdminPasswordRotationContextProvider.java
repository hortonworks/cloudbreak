package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.dto.StackDto;
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
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class CBCMAdminPasswordRotationContextProvider implements RotationContextProvider {

    private static final String DATETIMEFORMAT = "ddMMyyHHmmss";

    private static final String CB_USER_PREFIX = "cb";

    private static final String MGMT_USER_PREFIX = "mgmt";

    @Inject
    private StackDtoService stackService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resourceCrn);
        ClusterView cluster = stack.getCluster();

        String newCbUser = CB_USER_PREFIX + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());
        String newCbPassword = PasswordUtil.generatePassword();

        String newMgmtUser = MGMT_USER_PREFIX + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());
        String newMgmtPassword = PasswordUtil.generatePassword();

        Map<String, String> vaultPathMap = Maps.newHashMap();
        vaultPathMap.put(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret(), newCbUser);
        vaultPathMap.put(cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret(), newCbPassword);
        vaultPathMap.put(cluster.getDpClusterManagerUserSecret().getSecret(), newMgmtUser);
        vaultPathMap.put(cluster.getDpClusterManagerPasswordSecret().getSecret(), newMgmtPassword);
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withVaultPathSecretMap(vaultPathMap)
                .build();

        CMUserRotationContext cmUserRotationContext = CMUserRotationContext.builder()
                .withRotatableSecrets(Set.of(
                        Pair.of(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret(),
                                cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret()),
                        Pair.of(cluster.getDpClusterManagerUserSecret().getSecret(),
                                cluster.getDpClusterManagerPasswordSecret().getSecret())))
                .withClientUserSecret(cluster.getCloudbreakClusterManagerUserSecretObject().getSecret())
                .withClientPasswordSecret(cluster.getCloudbreakClusterManagerPasswordSecretObject().getSecret())
                .withResourceCrn(stack.getResourceCrn())
                .build();

        ClusterProxyReRegisterRotationContext clusterProxyReRegisterRotationContext = ClusterProxyReRegisterRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
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
