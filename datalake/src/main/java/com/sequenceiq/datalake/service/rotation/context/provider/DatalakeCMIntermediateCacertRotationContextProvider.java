package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_INTERMEDIATE_CA_CERT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;

@Component
public class DatalakeCMIntermediateCacertRotationContextProvider extends AbstractDatalakeRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return CM_INTERMEDIATE_CA_CERT;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.CM_INTERMEDIATE_CA_CERT);
    }
}
