package com.sequenceiq.cloudbreak.core.flow.handlers;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.service.ClusterFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.event.Event;

@Service
public class ClusterStatusUpdateFailureHandler  extends AbstractFlowHandler<StackStatusUpdateContext> {

    @Autowired
    private ClusterFacade clusterFacade;

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        StackStatusUpdateContext context = event.getData();
        if (context.isStart()) {
            clusterFacade.clusterStartError(context);
        } else {
            clusterFacade.clusterStopError(context);
        }
        return context;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        return null;
    }
}
