package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_IDBROKER_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.IDBROKER_CERT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;

@Component
public class DatalakeIdBrokerCertRotationContextProvider extends AbstractDatalakeRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return IDBROKER_CERT;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, INTERNAL_DATALAKE_IDBROKER_CERT);
    }
}
