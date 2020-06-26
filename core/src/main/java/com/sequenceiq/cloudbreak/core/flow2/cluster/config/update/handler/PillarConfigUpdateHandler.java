package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PillarConfigUpdateHandler extends ExceptionCatcherEventHandler<PillarConfigUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PillarConfigUpdateHandler.class);

    @Inject
    private PillarConfigUpdateService pillarConfigUpdateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PillarConfigUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new PillarConfigUpdateFailed(resourceId, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        PillarConfigUpdateRequest request = event.getData();
        Selectable response;
        try {
            pillarConfigUpdateService.doConfigUpdate(request.getResourceId());
            response = new PillarConfigUpdateSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.warn("Pillar configuration update failed.", e);
            response = new PillarConfigUpdateFailed(request.getResourceId(), e);
        }
        sendEvent(response, event);
    }
}
