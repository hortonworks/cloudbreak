package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType.ALL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType.HOST_CERTS;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SaltUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowEventProvider;

@Component
public class CloudbreakSecretRotationFlowEventProvider implements SecretRotationFlowEventProvider {

    @Override
    public Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event) {
        return new SaltUpdateTriggerEvent(event.getResourceId(), event.accepted(), skipSaltHighstate(event));
    }

    @Override
    public Set<Selectable> getPostFlowEvent(SecretRotationFlowChainTriggerEvent triggerEvent) {
        return triggerEvent.getSecretTypes().stream()
                .filter(secretType -> secretType.getFlags().contains(SecretTypeFlag.POST_FLOW))
                .map(secretType -> (CloudbreakSecretType) secretType)
                .map(secretTypeToPostFlowEvent(triggerEvent))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private static Function<CloudbreakSecretType, Optional<StackEvent>> secretTypeToPostFlowEvent(
            SecretRotationFlowChainTriggerEvent event) {
        return cloudbreakSecretType -> switch (cloudbreakSecretType) {
            case CM_INTERMEDIATE_CA_CERT -> getCertRotationEvent(ALL, event);
            case PRIVATE_HOST_CERTS -> getCertRotationEvent(HOST_CERTS, event);
            default -> Optional.empty();
        };
    }

    private static Optional<StackEvent> getCertRotationEvent(CertificateRotationType certificateRotationType,
            SecretRotationFlowChainTriggerEvent event) {
        if (event.getExecutionType() == null || event.getExecutionType().equals(RotationFlowExecutionType.ROTATE)) {
            return Optional.of(new ClusterCertificatesRotationTriggerEvent(ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.event(),
                    event.getResourceId(), certificateRotationType, Boolean.TRUE));
        }
        return Optional.empty();
    }
}
