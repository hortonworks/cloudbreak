package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_SHARED_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMServiceSharedDBPasswordRotationContextProvider extends AbstractCMRelatedDatabasePasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        StackDto stack = stackService.getByCrn(resource);
        Map<RDSConfig, Pair<String, String>> newUserPassPairs = getUserPassPairs(stack, resource);
        VaultRotationContext vaultRotationContext = getVaultRotationContext(newUserPassPairs, stack, resource);
        SaltPillarRotationContext pillarUpdateRotationContext = new SaltPillarRotationContext(stack.getResourceCrn(), this::getRotationPillarProperties);

        SaltStateApplyRotationContext.SaltStateApplyRotationContextBuilder saltStateApplyRotationContextBuilder =
                getSaltStateApplyRotationContextBuilder(stack);
        if (fullRotation(resource)) {
            saltStateApplyRotationContextBuilder = saltStateApplyRotationContextBuilder
                    .withStates(List.of("postgresql.rotate.init"))
                    .withRollbackStates(List.of("postgresql.rotate.rollback"))
                    .withCleanupStates(List.of("postgresql.rotate.finalize"));
        } else {
            saltStateApplyRotationContextBuilder = saltStateApplyRotationContextBuilder.withStates(List.of());
        }

        CMServiceConfigRotationContext cmServiceConfigRotationContext = getCMServiceConfigRotationContext(newUserPassPairs, stack);

        return Map.of(VAULT, vaultRotationContext,
                SALT_PILLAR, pillarUpdateRotationContext,
                SALT_STATE_APPLY, saltStateApplyRotationContextBuilder.build(),
                CM_SERVICE, cmServiceConfigRotationContext);
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }

    @Override
    protected boolean fullRotation(String resource) {
        return Objects.requireNonNull(Crn.fromString(resource)).getResourceType().equals(Crn.ResourceType.DATALAKE);
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigCountPredicate() {
        return rdsConfig -> rdsConfigService.getClustersUsingResource(rdsConfig).size() > 1;
    }

    @Override
    public SecretType getSecret() {
        return CLUSTER_CM_SERVICES_SHARED_DB_PASSWORD;
    }
}