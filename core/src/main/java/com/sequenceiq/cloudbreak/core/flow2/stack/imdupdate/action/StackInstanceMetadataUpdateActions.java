package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FINALIZED_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateTriggerEvent;

@Configuration
public class StackInstanceMetadataUpdateActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackInstanceMetadataUpdateActions.class);

    @Bean(name = "STACK_IMDUPDATE_STATE")
    public Action<?, ?> stackInstanceMetadataUpdate() {
        return new AbstractStackInstanceMetadataUpdateAction<>(StackInstanceMetadataUpdateTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext ctx, StackInstanceMetadataUpdateTriggerEvent payload, Map<Object, Object> variables) {
                try {
                    sendEvent(ctx, new StackInstanceMetadataUpdateRequest(ctx.getCloudContext(), ctx.getCloudCredential(),
                            ctx.getCloudStack(), payload.getUpdateType()));
                } catch (Exception e) {
                    LOGGER.error("Failed to update metadata of Stack instances: ", e);
                    StackInstanceMetadataUpdateFailureEvent failureEvent = new StackInstanceMetadataUpdateFailureEvent(payload.getResourceId(), e);
                    sendEvent(ctx, STACK_IMDUPDATE_FAILURE_EVENT.selector(), failureEvent);
                }
            }
        };
    }

    @Bean(name = "STACK_IMDUPDATE_FINISHED_STATE")
    public Action<?, ?> instanceMetadataUpdateFinished() {
        return new AbstractStackInstanceMetadataUpdateAction<>(StackInstanceMetadataUpdateResult.class) {

            @Override
            protected void doExecute(StackContext context, StackInstanceMetadataUpdateResult payload, Map<Object, Object> variables) {
                sendEvent(context, STACK_IMDUPDATE_FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "STACK_IMDUPDATE_FAILED_STATE")
    public Action<?, ?> instanceMetadataUpdateFailedAction() {
        return new AbstractStackInstanceMetadataUpdateAction<>(StackInstanceMetadataUpdateFailureEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackInstanceMetadataUpdateFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Instance metadata update failed with: ", payload.getException());
                sendEvent(context, STACK_IMDUPDATE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
