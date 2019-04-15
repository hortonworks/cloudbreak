package com.sequenceiq.cloudbreak.core.flow2;

import javax.annotation.PostConstruct;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;

public abstract class AbstractStackAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload>
        extends AbstractAction<S, E, C, P> {
    protected AbstractStackAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    public CloudbreakMetricService getMetricService() {
        return (CloudbreakMetricService) super.getMetricService();
    }
}
