package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;

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
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowEventProvider;

@Component
public class CloudbreakSecretRotationFlowEventProvider implements SecretRotationFlowEventProvider {

    @Override
    public Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event) {
        return new StackEvent(SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted());
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

    private static Function<CloudbreakSecretType, ClusterCertificatesRotationTriggerEvent> secretTypeToPostFlowEvent(
            SecretRotationFlowChainTriggerEvent event) {
        return cloudbreakSecretType -> switch (cloudbreakSecretType) {
            case DATAHUB_CM_INTERMEDIATE_CA_CERT ->
                    new ClusterCertificatesRotationTriggerEvent(ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.event(),
                            event.getResourceId(), CertificateRotationType.HOST_CERTS, Boolean.TRUE);
            default -> null;
        };
    }
}
