package com.sequenceiq.cloudbreak.rotation;

import java.util.Objects;
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
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static Function<CloudbreakSecretType, StackEvent> secretTypeToPostFlowEvent(
            SecretRotationFlowChainTriggerEvent event) {
        return cloudbreakSecretType -> switch (cloudbreakSecretType) {
            case CM_INTERMEDIATE_CA_CERT ->
                    new ClusterCertificatesRotationTriggerEvent(ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.event(),
                            event.getResourceId(), CertificateRotationType.ALL, Boolean.TRUE);
            default -> null;
        };
    }
}
