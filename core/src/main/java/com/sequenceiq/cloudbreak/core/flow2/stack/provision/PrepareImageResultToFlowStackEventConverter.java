package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;

public class PrepareImageResultToFlowStackEventConverter implements PayloadConverter<FlowStackEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return PrepareImageResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public FlowStackEvent convert(Object payload) {
        return new FlowStackEvent(((CloudPlatformResult) payload).getRequest().getCloudContext().getId());
    }
}
