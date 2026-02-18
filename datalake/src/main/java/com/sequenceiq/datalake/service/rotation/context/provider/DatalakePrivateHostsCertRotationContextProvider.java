package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.sdx.rotation.DatalakeSecretType.PRIVATE_HOST_CERTS;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;

@Component
public class DatalakePrivateHostsCertRotationContextProvider extends AbstractDatalakeRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return PRIVATE_HOST_CERTS;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.PRIVATE_HOST_CERTS);
    }
}
