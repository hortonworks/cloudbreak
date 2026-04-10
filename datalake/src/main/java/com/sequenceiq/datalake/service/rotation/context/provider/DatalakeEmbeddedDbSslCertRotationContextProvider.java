package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeEmbeddedDbSslCertRotationContextProvider extends DatalakeConditionalRotationContextProvider {

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.EMBEDDED_DB_SSL_CERT;
    }

    @Override
    public Map<RotationSource, SecretType> getPollingTypes() {
        return Map.of(RotationSource.CLOUDBREAK, CloudbreakSecretType.EMBEDDED_DB_SSL_CERT);
    }

    @Override
    protected Function<SdxCluster, Boolean> getConditionalRotationFunction() {
        return sdxCluster -> !SdxDatabaseAvailabilityType.hasExternalDatabase(sdxCluster.getDatabaseAvailabilityType());
    }
}
