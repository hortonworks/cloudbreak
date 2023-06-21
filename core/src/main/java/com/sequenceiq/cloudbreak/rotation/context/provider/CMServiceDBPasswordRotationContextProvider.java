package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.VAULT;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class CMServiceDBPasswordRotationContextProvider implements RotationContextProvider {

    private static final Integer SALT_STATE_MAX_RETRY = 3;

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private List<AbstractRdsRoleConfigProvider> rdsRoleConfigProviders;

    @Inject
    private List<AbstractRdsConfigProvider> rdsConfigProviders;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();
        Set<RDSConfig> rdsConfigs = getRdsConfigs(cluster);
        Map<RDSConfig, Pair<String, String>> newUserPassPairs = rdsConfigs.stream()
                .filter(rdsConfig -> rdsConfigProviders.stream().anyMatch(configProvider -> matchRdsTypeWithString(configProvider.getRdsType(), rdsConfig)))
                .collect(Collectors.toMap(rdsConfig -> rdsConfig, this::getUserPassPairs));

        Map<String, String> secretMap = Maps.newHashMap();
        newUserPassPairs.forEach((rdsConfig, userPassPair) -> {
            secretMap.put(rdsConfig.getConnectionUserNameSecret(), userPassPair.getKey());
            secretMap.put(rdsConfig.getConnectionPasswordSecret(), userPassPair.getValue());
        });

        Table<String, String, String> cmServiceConfigTable = HashBasedTable.create();
        newUserPassPairs.forEach((rdsConfig, userPassPair) -> updateCmServiceConfigToTableIfExists(cmServiceConfigTable, rdsConfig,
                userPassPair.getKey(), userPassPair.getValue()));

        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(secretMap)
                .withResourceCrn(stack.getResourceCrn())
                .build();

        SaltPillarRotationContext pillarUpdateRotationContext = new SaltPillarRotationContext(stack.getResourceCrn(), this::getRotationPillarProperties);

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        SaltStateApplyRotationContext stateApplyRotationContext = SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(primaryGatewayConfig.getHostname()))
                .withExitCriteriaModel(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel())
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY)
                .withStates(List.of("postgresql.rotate.init"))
                .withRollbackStates(List.of("postgresql.rotate.rollback"))
                .withCleanupStates(List.of("postgresql.rotate.finalize"))
                .withPreValidateStates(List.of("postgresql.rotate.prevalidate"))
                .withPostValidateStates(List.of("postgresql.rotate.postvalidate"))
                .build();

        CMServiceConfigRotationContext cmServiceConfigRotationContext = new CMServiceConfigRotationContext(stack.getResourceCrn(), cmServiceConfigTable);

        result.put(VAULT, vaultRotationContext);
        result.put(SALT_PILLAR, pillarUpdateRotationContext);
        result.put(SALT_STATE_APPLY, stateApplyRotationContext);
        result.put(CM_SERVICE, cmServiceConfigRotationContext);
        return result;
    }

    @NotNull
    private Pair<String, String> getUserPassPairs(RDSConfig rdsConfig) {
        AbstractRdsConfigProvider abstractRdsConfigProvider = rdsConfigProviders.stream()
                .filter(configProvider -> matchRdsTypeWithString(configProvider.getRdsType(), rdsConfig)).findFirst().get();
        String newUser = abstractRdsConfigProvider.getDbUser() + new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
        String newPassword = PasswordUtil.generatePassword();
        return Pair.of(newUser, newPassword);
    }

    private boolean matchRdsTypeWithString(DatabaseType rdsType, RDSConfig rdsConfig) {
        if (Arrays.stream(DatabaseType.values()).anyMatch(value -> StringUtils.equals(value.name(), rdsConfig.getType()))) {
            return rdsType.equals(DatabaseType.valueOf(rdsConfig.getType()));
        }
        return false;
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

    private Map<String, SaltPillarProperties> getRotationPillarProperties(String resource) {
        try {
            StackDto stack = stackService.getByCrn(resource);
            Map<String, SaltPillarProperties> pillarPropertiesSet = Maps.newHashMap();
            pillarPropertiesSet.put(PostgresConfigService.POSTGRESQL_SERVER,
                    postgresConfigService.getPostgreSQLServerPropertiesForRotation(stack));
            pillarPropertiesSet.put(PostgresConfigService.POSTGRES_ROTATION, postgresConfigService.getPillarPropertiesForRotation(stack));
            return pillarPropertiesSet;
        } catch (Exception e) {
            throw new CloudbreakServiceException("Failed to generate pillar properties for CM DB password rotation.", e);
        }
    }

    private Set<RDSConfig> getRdsConfigs(ClusterView cluster) {
        Set<DatabaseType> relatedRdsRoleTypes = rdsRoleConfigProviders.stream().map(AbstractRdsRoleConfigProvider::dbType).collect(Collectors.toSet());
        Set<DatabaseType> relatedRdsTypes = rdsConfigProviders.stream().map(AbstractRdsConfigProvider::getRdsType).collect(Collectors.toSet());
        return rdsConfigService.findByClusterId(cluster.getId())
                .stream()
                .filter(rdsConfig -> !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType()))
                .filter(rdsConfig -> rdsConfigService.getClustersUsingResource(rdsConfig).size() == 1)
                .collect(Collectors.toSet());
    }

    @Override
    public SecretType getSecret() {
        return CLUSTER_CM_SERVICES_DB_PASSWORD;
    }
}