package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DisableKerberosResult;

public class DisableKerberosResultToStackEventConverter implements PayloadConverter<StackEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DisableKerberosResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackEvent convert(Object payload) {
        return new StackEvent(((ClusterPlatformResult<?>) payload).getRequest().getStackId());
    }
}
