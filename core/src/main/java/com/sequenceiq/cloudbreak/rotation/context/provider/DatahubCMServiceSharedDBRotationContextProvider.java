package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatahubCMServiceSharedDBRotationContextProvider extends AbstractCMRelatedDatabasePasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        CustomJobRotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withPreValidateJob(() -> {
                    throw new NotImplementedException("This rotation will be reworked soon...");
                })
                .withResourceCrn(resourceCrn)
                .build();
        return Map.of(CUSTOM_JOB, customJobRotationContext);
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> StringUtils.equals(rdsConfig.getType(), HIVE.name());
    }

    @Override
    public SecretType getSecret() {
        return CM_SERVICE_SHARED_DB;
    }
}
