package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.CB_CLUSTER_MANAGER_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.CB_CLUSTER_MANAGER_USER;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.DP_CLUSTER_MANAGER_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.DP_CLUSTER_MANAGER_USER;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.RDS_CONFIG_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.RDS_CONFIG_USERNAME;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_BOOT_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_MASTER_PRIVATE_KEY;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_SIGN_PRIVATE_KEY;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.rotation.secret.vault.SyncSecretVersionService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Component
public class OutdatedVaultSecretUpdaterPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutdatedVaultSecretUpdaterPatchService.class);

    @Value("${existing-stack-patcher.patch-configs.update-outdated-vault-secrets.intervalMinutes}")
    private int intervalInMinutes;

    @Inject
    private ClusterService clusterService;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private SyncSecretVersionService syncSecretVersionService;

    @Override
    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.UPDATE_OUTDATED_VAULT_SECRETS;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return true;
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        updateOutdatedSecretsForCluster(stack);
        updateOutdatedSecretsForSaltSecurityConfig(stack);
        updateOutdatedSecretsForRdsConfigs(stack);
        if (StackType.DATALAKE.equals(stack.getType())) {
            ThreadBasedUserCrnProvider.doAsInternalActor(() -> freeIpaRotationV1Endpoint.syncOutdatedSecrets(stack.getEnvironmentCrn()));
        }
        if (StringUtils.isNotEmpty(stack.getCluster().getDatabaseServerCrn())) {
            ThreadBasedUserCrnProvider.doAsInternalActor(() -> databaseServerV4Endpoint.syncOutdatedSecrets(stack.getCluster().getDatabaseServerCrn()));
        }
        return true;
    }

    private void updateOutdatedSecretsForCluster(Stack stack) {
        Cluster cluster = clusterService.getCluster(stack.getClusterId());
        syncSecretVersionService.updateEntityIfNeeded(stack.getResourceCrn(), cluster,
                Set.of(CB_CLUSTER_MANAGER_USER, CB_CLUSTER_MANAGER_PASSWORD, DP_CLUSTER_MANAGER_USER, DP_CLUSTER_MANAGER_PASSWORD));
    }

    private void updateOutdatedSecretsForSaltSecurityConfig(Stack stack) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (securityConfig != null && securityConfig.getSaltSecurityConfig() != null) {
            Optional<SaltSecurityConfig> saltSecurityConfig = saltSecurityConfigService.getById(securityConfig.getSaltSecurityConfig().getId());
            saltSecurityConfig.ifPresent(ssc -> syncSecretVersionService.updateEntityIfNeeded(stack.getResourceCrn(), ssc,
                    Set.of(SALT_PASSWORD, SALT_MASTER_PRIVATE_KEY, SALT_SIGN_PRIVATE_KEY, SALT_BOOT_PASSWORD)));
        }
    }

    private void updateOutdatedSecretsForRdsConfigs(Stack stack) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(stack.getClusterId());
        rdsConfigs.forEach(rdsConfig ->
                syncSecretVersionService.updateEntityIfNeeded(stack.getResourceCrn(), rdsConfig, Set.of(RDS_CONFIG_USERNAME, RDS_CONFIG_PASSWORD)));
    }
}
