package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_SSSD_IPA_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_KERBEROS_BIND_USER;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeSssdIpaPasswordSecretRotationContextProvider implements RotationContextProvider {

    @Inject
    private SdxService sdxService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        PollerRotationContext freeipaPollerRotationContext = new PollerRotationContext(resourceCrn, FREEIPA_KERBEROS_BIND_USER,
                Map.of(CLUSTER_NAME.name(), sdxCluster.getName()));
        return Map.of(CLOUDBREAK_ROTATE_POLLING, new PollerRotationContext(resourceCrn, INTERNAL_DATALAKE_SSSD_IPA_PASSWORD),
                FREEIPA_ROTATE_POLLING, freeipaPollerRotationContext);
    }

    @Override
    public SecretType getSecret() {
        return DatalakeSecretType.DATALAKE_SSSD_IPA_PASSWORD;
    }
}
