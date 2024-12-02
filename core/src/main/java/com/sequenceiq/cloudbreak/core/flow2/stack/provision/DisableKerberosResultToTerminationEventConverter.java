package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType.FORCED;
import static com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType.REGULAR;

import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.AbstractClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class DisableKerberosResultToTerminationEventConverter implements PayloadConverter<TerminationEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DisableKerberosResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public TerminationEvent convert(Object payload) {
        AbstractClusterTerminationRequest request = (AbstractClusterTerminationRequest) ((ClusterPlatformResult<?>) payload).getRequest();
        return new TerminationEvent(request.getSelector(), request.getResourceId(), request.isForced() ? FORCED : REGULAR);
    }
}
