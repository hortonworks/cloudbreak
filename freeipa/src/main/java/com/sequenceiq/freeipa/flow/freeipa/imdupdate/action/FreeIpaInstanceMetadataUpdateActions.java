package com.sequenceiq.freeipa.flow.freeipa.imdupdate.action;

import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FINALIZED_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateResult;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Configuration
public class FreeIpaInstanceMetadataUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceMetadataUpdateActions.class);

    @Bean(name = "STACK_IMDUPDATE_STATE")
    public Action<?, ?> stackInstanceMetadataUpdate() {
        return new AbstractFreeIpaInstanceMetadataUpdateAction<>(FreeIpaInstanceMetadataUpdateTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext ctx, FreeIpaInstanceMetadataUpdateTriggerEvent payload, Map<Object, Object> variables) {
                sendEvent(ctx, new FreeIpaInstanceMetadataUpdateRequest(ctx.getCloudContext(), ctx.getCloudCredential(),
                        ctx.getCloudStack(), payload.getUpdateType()));
            }
        };
    }

    @Bean(name = "STACK_IMDUPDATE_FINISHED_STATE")
    public Action<?, ?> instanceMetadataUpdateFinished() {
        return new AbstractFreeIpaInstanceMetadataUpdateAction<>(FreeIpaInstanceMetadataUpdateResult.class) {

            @Override
            protected void doExecute(StackContext context, FreeIpaInstanceMetadataUpdateResult payload, Map<Object, Object> variables) {
                sendEvent(context, STACK_IMDUPDATE_FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "STACK_IMDUPDATE_FAILED_STATE")
    public Action<?, ?> instanceMetadataUpdateFailedAction() {
        return new AbstractFreeIpaInstanceMetadataUpdateAction<>(FreeIpaInstanceMetadataUpdateFailureEvent.class) {

            @Override
            protected void doExecute(StackContext context, FreeIpaInstanceMetadataUpdateFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Instance metadata update failed with: ", payload.getException());
                sendEvent(context, STACK_IMDUPDATE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
