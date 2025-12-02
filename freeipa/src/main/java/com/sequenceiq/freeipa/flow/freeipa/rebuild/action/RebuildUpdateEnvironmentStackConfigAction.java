package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

@Component("RebuildUpdateEnvironmentStackConfigAction")
public class RebuildUpdateEnvironmentStackConfigAction extends AbstractRebuildAction<StackEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildUpdateEnvironmentStackConfigAction.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    protected RebuildUpdateEnvironmentStackConfigAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating clusters' configuration");
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> environmentEndpoint.updateConfigsInEnvironmentByCrn(context.getStack().getEnvironmentCrn()));
            sendEvent(context, new StackEvent(UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT.event(), payload.getResourceId()));
        } catch (ClientErrorException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to update the clusters' config due to {}", errorMessage, e);
            sendEvent(context, new RebuildFailureEvent(payload.getResourceId(), ERROR,
                    new Exception("Failed to update the clusters' config due to " + errorMessage, e)));
        } catch (Exception e) {
            LOGGER.error("Failed to update the clusters' config", e);
            sendEvent(context, new RebuildFailureEvent(payload.getResourceId(), ERROR,
                    new Exception("Failed to update the clusters' config due to " + e.getMessage(), e)));
        }
    }
}
