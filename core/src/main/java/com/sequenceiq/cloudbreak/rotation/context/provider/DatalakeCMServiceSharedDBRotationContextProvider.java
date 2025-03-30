package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.DatahubSharedServiceRotationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatalakeCMServiceSharedDBRotationContextProvider extends CMServiceDBPasswordRotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeCMServiceSharedDBRotationContextProvider.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private DatahubSharedServiceRotationService datahubSharedServiceRotationService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>(super.getContexts(resourceCrn));
        StackDto datalake = stackService.getByCrn(resourceCrn);
        CustomJobRotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withPreValidateJob(() -> datahubSharedServiceRotationService.validateAllDatahubAvailable(datalake))
                .withRotationJob(() -> datahubSharedServiceRotationService.updateAllRelevantDatahub(datalake))
                .withResourceCrn(resourceCrn)
                .build();
        contexts.put(CUSTOM_JOB, customJobRotationContext);
        return contexts;
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> StringUtils.equals(rdsConfig.getType(), DatabaseType.HIVE.name());
    }

    @Override
    public SecretType getSecret() {
        return INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB;
    }
}
