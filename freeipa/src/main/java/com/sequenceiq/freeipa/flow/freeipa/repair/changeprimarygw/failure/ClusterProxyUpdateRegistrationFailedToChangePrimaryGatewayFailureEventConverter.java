package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;

public class ClusterProxyUpdateRegistrationFailedToChangePrimaryGatewayFailureEventConverter implements PayloadConverter<ChangePrimaryGatewayFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterProxyUpdateRegistrationFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ChangePrimaryGatewayFailureEvent convert(Object payload) {
        ClusterProxyUpdateRegistrationFailed registrationFailed = (ClusterProxyUpdateRegistrationFailed) payload;
        ChangePrimaryGatewayFailureEvent event = new ChangePrimaryGatewayFailureEvent(registrationFailed.getResourceId(),
                "Change Primary Gateway Cluster Proxy Registration",
                Set.of(), Map.of(), new Exception("Payload failed: " + payload));
        return event;
    }
}
