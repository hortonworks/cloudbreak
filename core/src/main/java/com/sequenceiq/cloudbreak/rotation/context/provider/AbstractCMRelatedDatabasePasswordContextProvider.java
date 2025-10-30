package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

public abstract class AbstractCMRelatedDatabasePasswordContextProvider {

    private static final Integer SALT_STATE_MAX_RETRY = 3;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private List<AbstractRdsConfigProvider> rdsConfigProviders;

    @Inject
    private List<AbstractRdsRoleConfigProvider> rdsRoleConfigProviders;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    protected abstract Predicate<RDSConfig> getRDSConfigTypePredicate();

    protected Map<String, SaltPillarProperties> getPillarProperties(StackDto stack) {
        try {
            Map<String, SaltPillarProperties> pillarPropertiesSet = Maps.newHashMap();
            pillarPropertiesSet.put(PostgresConfigService.POSTGRESQL_SERVER, postgresConfigService.getPostgreSQLServerPropertiesForRotation(stack));
            pillarPropertiesSet.put(PostgresConfigService.POSTGRES_ROTATION, postgresConfigService.getPillarPropertiesForRotation(stack));
            return pillarPropertiesSet;
        } catch (Exception e) {
            throw new CloudbreakServiceException("Failed to generate pillar properties for DB username/password rotation.", e);
        }
    }

    protected CMServiceConfigRotationContext getCMServiceConfigRotationContext(Map<RDSConfig, Pair<String, String>> newUserPassPairs, StackDto stack) {
        Table<String, String, String> cmServiceConfigTable = HashBasedTable.create();
        newUserPassPairs.forEach((rdsConfig, userPassPair) -> updateCmServiceConfigToTableIfExists(cmServiceConfigTable, rdsConfig,
                userPassPair.getKey(), userPassPair.getValue()));
        return new CMServiceConfigRotationContext(stack.getResourceCrn(), cmServiceConfigTable);
    }

    private void updateCmServiceConfigToTableIfExists(Table<String, String, String> cmServiceConfigTable, RDSConfig rdsConfig,
            String newUser, String newPassword) {
        Optional<AbstractRdsRoleConfigProvider> abstractRdsRoleConfigProvider = rdsRoleConfigProviders.stream()
                .filter(configProvider -> configProvider.dbType().equals(DatabaseType.valueOf(rdsConfig.getType()))).findFirst();
        if (abstractRdsRoleConfigProvider.isPresent()) {
            AbstractRdsRoleConfigProvider roleConfigProvider = abstractRdsRoleConfigProvider.get();
            cmServiceConfigTable.put(roleConfigProvider.getServiceType(), roleConfigProvider.dbUserKey(), newUser);
            cmServiceConfigTable.put(roleConfigProvider.getServiceType(), roleConfigProvider.dbPasswordKey(), newPassword);
        }
    }

    protected VaultRotationContext getVaultRotationContext(Map<RDSConfig, Pair<String, String>> userPassPairs, StackDto stack) {
        Map<RDSConfig, Map<SecretMarker, String>> newSecretMap = Maps.newHashMap();
        userPassPairs.forEach((rdsConfig, userPassPair) -> {
            newSecretMap.put(rdsConfig, Map.of(
                    SecretMarker.RDS_CONFIG_USERNAME, userPassPair.getKey(),
                    SecretMarker.RDS_CONFIG_PASSWORD, userPassPair.getValue()));
        });
        return VaultRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withNewSecretMap(newSecretMap)
                .build();
    }

    protected SaltStateApplyRotationContext.SaltStateApplyRotationContextBuilder getSaltStateApplyRotationContextBuilder(StackDto stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(primaryGatewayConfig.getHostname()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY);
    }

    protected Map<RDSConfig, Pair<String, String>> getUserPassPairs(StackDto stack) {
        ClusterView cluster = stack.getCluster();
        return rdsConfigService.findByClusterId(cluster.getId())
                .stream()
                .filter(getRDSConfigTypePredicate())
                .filter(rdsConfig -> rdsConfigProviders.stream().anyMatch(configProvider -> matchRdsTypeWithString(configProvider.getRdsType(), rdsConfig)))
                .collect(Collectors.toMap(rdsConfig -> rdsConfig, this::getUserPassPairs));
    }

    private boolean matchRdsTypeWithString(DatabaseType rdsType, RDSConfig rdsConfig) {
        return rdsType.name().equals(rdsConfig.getType());
    }

    private String getDefaultUserName(RDSConfig rdsConfig) {
        return rdsConfigProviders.stream()
                .filter(configProvider -> matchRdsTypeWithString(configProvider.getRdsType(), rdsConfig))
                .map(AbstractRdsConfigProvider::getDbUser)
                .findFirst()
                .orElseThrow();
    }

    private Pair<String, String> getUserPassPairs(RDSConfig rdsConfig) {
        RotationSecret user = uncachedSecretServiceForRotation.getRotation(rdsConfig.getConnectionUserNameSecret());
        RotationSecret password = uncachedSecretServiceForRotation.getRotation(rdsConfig.getConnectionPasswordSecret());
        if (user.isRotation() && password.isRotation()) {
            return Pair.of(user.getSecret(), password.getSecret());
        } else if (!user.isRotation() && !password.isRotation()) {
            String newUser = getDefaultUserName(rdsConfig) + new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
            String newPassword = PasswordUtil.generatePassword();
            return Pair.of(newUser, newPassword);
        } else {
            throw new CloudbreakServiceException("Only one of secret is under rotation from user and password, which is unexpected.");
        }
    }
}
