package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_SERVICE_SHARED_DB;

import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
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
        throw new SecretRotationException("This rotation will be reimplemented to use different HMS DB user/password per cluster!");
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> false;
    }

    @Override
    public SecretType getSecret() {
        return CM_SERVICE_SHARED_DB;
    }
}
