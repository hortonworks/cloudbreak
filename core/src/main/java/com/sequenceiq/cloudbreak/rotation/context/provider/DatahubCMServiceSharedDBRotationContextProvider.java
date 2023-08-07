package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_SERVICE_SHARED_DB;

import java.util.Map;
import java.util.Set;
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
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatahubCMServiceSharedDBRotationContextProvider extends AbstractCMRelatedDatabasePasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        Map<RDSConfig, Pair<String, String>> userPassPairs = getUserPassPairs(stack);
        SaltPillarRotationContext pillarUpdateRotationContext = new SaltPillarRotationContext(stack.getResourceCrn(), this::getPillarProperties);
        CMServiceConfigRotationContext cmServiceConfigRotationContext = getCMServiceConfigRotationContext(userPassPairs, stack);
        return Map.of(SALT_PILLAR, pillarUpdateRotationContext,
                CM_SERVICE, cmServiceConfigRotationContext);
    }

    @Override
    public Set<String> getVaultSecretsForRollback(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        Map<RDSConfig, Pair<String, String>> userPassPairs = getUserPassPairs(stack);
        return getVaultRotationContext(userPassPairs, stack).getVaultPathSecretMap().keySet();
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigCountPredicate() {
        return rdsConfig -> rdsConfigService.getClustersUsingResource(rdsConfig).size() > 1;
    }

    @Override
    public SecretType getSecret() {
        return DATAHUB_CM_SERVICE_SHARED_DB;
    }
}