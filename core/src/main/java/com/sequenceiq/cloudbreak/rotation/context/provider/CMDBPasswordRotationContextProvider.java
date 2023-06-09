package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.VAULT;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class CMDBPasswordRotationContextProvider implements RotationContextProvider {

    private static final Integer SALT_STATE_MAX_RETRY = 3;

    @Value("${cb.clouderamanager.service.database.user:clouderamanager}")
    private String defaultUserName;

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();
        RDSConfig cmRdsConfig = getRdsConfig(cluster);

        String newUser = defaultUserName + new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
        String newPassword = PasswordUtil.generatePassword();
        Map<String, String> secretMap = Map.of(cmRdsConfig.getConnectionUserNameSecret(), newUser, cmRdsConfig.getConnectionPasswordSecret(), newPassword);
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
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withMaxRetry(SALT_STATE_MAX_RETRY)
                .withMaxRetryOnError(SALT_STATE_MAX_RETRY)
                .withStates(List.of("cloudera.manager.server-stop", "postgresql.rotate.init",
                        "cloudera.manager.rotate.rotate-secrets", "cloudera.manager.server-start"))
                .withRollbackStates(List.of("cloudera.manager.server-stop", "postgresql.rotate.rollback",
                        "cloudera.manager.rotate.rollback-secrets", "cloudera.manager.server-start"))
                .withCleanupStates(List.of("postgresql.rotate.finalize"))
                .withPreValidateStates(List.of("postgresql.rotate.prevalidate"))
                .withPostValidateStates(List.of("postgresql.rotate.postvalidate"))
                .build();

        CustomJobRotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withRotationJob(() -> waitForClouderaManagerToStartup(stack))
                .withRollbackJob(() -> waitForClouderaManagerToStartup(stack))
                .build();

        result.put(VAULT, vaultRotationContext);
        result.put(SALT_PILLAR, pillarUpdateRotationContext);
        result.put(SALT_STATE_APPLY, stateApplyRotationContext);
        result.put(CUSTOM_JOB, customJobRotationContext);
        return result;
    }

    private void waitForClouderaManagerToStartup(StackDto stack) {
        try {
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            connector.clusterSetupService().waitForServer(false);
        } catch (Exception e) {
            throw new SecretRotationException(e, CUSTOM_JOB);
        }
    }

    private Map<String, SaltPillarProperties> getRotationPillarProperties(String resource) {
        try {
            StackDto stack = stackService.getByCrn(resource);
            Map<String, SaltPillarProperties> pillarPropertiesSet = Maps.newHashMap();
            pillarPropertiesSet.put(PostgresConfigService.POSTGRESQL_SERVER,
                    postgresConfigService.getPostgreSQLServerPropertiesForRotation(stack));
            pillarPropertiesSet.put(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY,
                    clusterHostServiceRunner.getClouderaManagerDatabasePillarProperties(stack.getCluster()));
            pillarPropertiesSet.put(PostgresConfigService.POSTGRES_ROTATION, postgresConfigService.getPillarPropertiesForRotation(stack));
            return pillarPropertiesSet;
        } catch (Exception e) {
            throw new CloudbreakServiceException("Failed to generate pillar properties for CM DB password rotation.", e);
        }
    }

    private RDSConfig getRdsConfig(ClusterView cluster) {
        Optional<RDSConfig> rds = rdsConfigService.findByClusterId(cluster.getId())
                .stream()
                .filter(rdsConfig -> DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType()))
                .findFirst();

        if (rds.isEmpty()) {
            throw new CloudbreakServiceException("There is no rds config for CM");
        }
        return rds.get();
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD;
    }
}