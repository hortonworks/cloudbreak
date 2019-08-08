package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosResult;
import com.sequenceiq.flow.core.PayloadConverter;

public class DisableKerberosResultToStackFailureEventConverter implements PayloadConverter<StackFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DisableKerberosResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackFailureEvent convert(Object payload) {
        return new StackFailureEvent(((ClusterPlatformResult<?>) payload).getRequest().getResourceId(), ((ClusterPlatformResult<?>) payload).getErrorDetails());
    }
}
