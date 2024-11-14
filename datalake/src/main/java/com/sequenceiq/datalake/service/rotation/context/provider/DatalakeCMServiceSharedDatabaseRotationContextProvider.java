package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeCMServiceSharedDatabaseRotationContextProvider implements RotationContextProvider {

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        throw new SecretRotationException("This rotation will be reimplemented to use different HMS DB user/password per cluster!");
    }

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.CM_SERVICE_SHARED_DB;
    }
}
