package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeUserKeyPairRotationContextProvider extends AbstractDatalakeRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.USER_KEYPAIR;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.USER_KEYPAIR);
    }
}
