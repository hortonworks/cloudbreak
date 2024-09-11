package com.sequenceiq.datalake.service.rotation;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowEventProvider;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxStartCertRotationEvent;
import com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateTriggerEvent;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeSecretRotationFlowEventProvider implements SecretRotationFlowEventProvider {

    @Override
    public Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event) {
        return new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(),
                ThreadBasedUserCrnProvider.getUserCrn(), event.accepted());
    }

    @Override
    public Set<Selectable> getPostFlowEvent(SecretRotationFlowChainTriggerEvent triggerEvent) {
        return triggerEvent.getSecretTypes().stream()
                .filter(secretType -> secretType.getFlags().contains(SecretTypeFlag.POST_FLOW))
                .map(secretType -> (DatalakeSecretType) secretType)
                .map(secretTypeToPostFlowEvent(triggerEvent))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Function<DatalakeSecretType, SdxEvent> secretTypeToPostFlowEvent(SecretRotationFlowChainTriggerEvent event) {
        return datalakeSecretType -> switch (datalakeSecretType) {
            case CM_INTERMEDIATE_CA_CERT -> {
                CertificatesRotationV4Request certificatesRotationV4Request = new CertificatesRotationV4Request();
                certificatesRotationV4Request.setSkipSaltUpdate(Boolean.TRUE);
                certificatesRotationV4Request.setCertificateRotationType(CertificateRotationType.ALL);
                yield new SdxStartCertRotationEvent(SdxCertRotationEvent.ROTATE_CERT_EVENT.event(), event.getResourceId(),
                        ThreadBasedUserCrnProvider.getUserCrn(), certificatesRotationV4Request);
            }
            default -> null;
        };
    }
}
