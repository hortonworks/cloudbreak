package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas.CheckAtlasUpdatedSaltEvent.CHECK_ATLAS_UPDATED_SALT_SUCCESS_HANDLED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.CheckAtlasUpdatedTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedSaltFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedSaltSuccessEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class CheckAtlasUpdatedSaltActions {
    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "CHECK_ATLAS_UPDATED_SALT_STATE")
    public Action<?, ?> checkAtlasUpdated() {
        return new AbstractCheckAtlasUpdatedSaltActions<>(CheckAtlasUpdatedTriggerEvent.class) {
            @Override
            protected void doExecute(CheckAtlasUpdatedSaltContext context, CheckAtlasUpdatedTriggerEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(
                        context.getStackId(), DetailedStackStatus.CHECK_ATLAS_UPDATED_IN_PROGRESS,
                        "Initiating Atlas up-to-date check"
                );
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(CheckAtlasUpdatedSaltContext context) {
                return new CheckAtlasUpdatedRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CHECK_ATLAS_UPDATED_SALT_SUCCESS_STATE")
    public Action<?, ?> checkAtlasUpdatedSaltSuccessAction() {
        return new AbstractCheckAtlasUpdatedSaltActions<>(CheckAtlasUpdatedSaltSuccessEvent.class) {
            @Override
            protected void doExecute(CheckAtlasUpdatedSaltContext context, CheckAtlasUpdatedSaltSuccessEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(
                        context.getStackId(), DetailedStackStatus.CHECK_ATLAS_UPDATED_FINISHED,
                        "Atlas up-to-date check successfully finished"
                );
                sendEvent(context, CHECK_ATLAS_UPDATED_SALT_SUCCESS_HANDLED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "CHECK_ATLAS_UPDATED_SALT_FAILED_STATE")
    public Action<?, ?> checkAtlasUpdatedSaltFailedAction() {
        return new AbstractCheckAtlasUpdatedSaltActions<>(CheckAtlasUpdatedSaltFailedEvent.class) {
            @Override
            protected void doExecute(CheckAtlasUpdatedSaltContext context, CheckAtlasUpdatedSaltFailedEvent payload, Map<Object, Object> variables) {
                getFlow(context.getFlowId()).setFlowFailed(payload.getException());
                stackUpdater.updateStackStatus(
                        context.getStackId(), DetailedStackStatus.CHECK_ATLAS_UPDATED_FAILED,
                        "Atlas up-to-date check failed"
                );
                sendEvent(context, CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
