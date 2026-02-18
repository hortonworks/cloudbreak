package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeEmbeddedDbSslCertRotationContextProvider extends AbstractDatalakeRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.EMBEDDED_DB_SSL_CERT;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.EMBEDDED_DB_SSL_CERT);
    }
}
