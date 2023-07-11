package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
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
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMServiceDBPasswordRotationContextProvider extends AbstractCMRelatedDatabasePasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        Map<RDSConfig, Pair<String, String>> newUserPassPairs = getUserPassPairs(stack, resourceCrn);
        VaultRotationContext vaultRotationContext = getVaultRotationContext(newUserPassPairs, stack, resourceCrn);
        SaltPillarRotationContext pillarUpdateRotationContext = new SaltPillarRotationContext(stack.getResourceCrn(), this::getPillarProperties);
        SaltStateApplyRotationContext stateApplyRotationContext = getSaltStateApplyRotationContextBuilder(stack)
                .withStates(List.of("postgresql.rotate.init"))
                .withRollbackStates(List.of("postgresql.rotate.rollback"))
                .withCleanupStates(List.of("postgresql.rotate.finalize"))
                .withPreValidateStates(List.of("postgresql.rotate.prevalidate"))
                .withPostValidateStates(List.of("postgresql.rotate.postvalidate"))
                .build();
        CMServiceConfigRotationContext cmServiceConfigRotationContext = getCMServiceConfigRotationContext(newUserPassPairs, stack);

        return Map.of(VAULT, vaultRotationContext,
                SALT_PILLAR, pillarUpdateRotationContext,
                SALT_STATE_APPLY, stateApplyRotationContext,
                CM_SERVICE, cmServiceConfigRotationContext);
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }

    @Override
    public SecretType getSecret() {
        return CLUSTER_CM_SERVICES_DB_PASSWORD;
    }
}