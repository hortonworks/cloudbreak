package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMDBPasswordRotationContextProvider extends AbstractCMRelatedDatabasePasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        StackDto stack = stackService.getByCrn(resource);
        Map<RDSConfig, Pair<String, String>> newUserPassPairs = getUserPassPairs(stack, resource);
        VaultRotationContext vaultRotationContext = getVaultRotationContext(newUserPassPairs, stack, resource);
        SaltPillarRotationContext pillarUpdateRotationContext = new SaltPillarRotationContext(stack.getResourceCrn(), this::getRotationPillarProperties);
        SaltStateApplyRotationContext stateApplyRotationContext = getSaltStateApplyRotationContextBuilder(stack)
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
        return Map.of(VAULT, vaultRotationContext,
                SALT_PILLAR, pillarUpdateRotationContext,
                SALT_STATE_APPLY, stateApplyRotationContext,
                CUSTOM_JOB, customJobRotationContext);
    }

    @Override
    protected void addPillarPropertiesIfNeeded(Map<String, SaltPillarProperties> pillarPropertiesSet, StackDto stack)
            throws CloudbreakOrchestratorFailedException {
        pillarPropertiesSet.put(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY,
                clusterHostServiceRunner.getClouderaManagerDatabasePillarProperties(stack.getCluster()));
    }

    private void waitForClouderaManagerToStartup(StackDto stack) {
        try {
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            connector.clusterSetupService().waitForServer(false);
        } catch (Exception e) {
            throw new SecretRotationException(e, CUSTOM_JOB);
        }
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD;
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }
}