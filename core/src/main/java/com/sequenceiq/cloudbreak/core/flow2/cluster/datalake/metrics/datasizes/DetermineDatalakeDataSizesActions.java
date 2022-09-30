package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesSuccessEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.GetDatalakeDataSizesRequest;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class DetermineDatalakeDataSizesActions {
    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS_STATE")
    public Action<?, ?> determineDatalakeDataSizesAction() {
        return new AbstractDetermineDatalakeDataSizesAction<>(StackEvent.class) {
            @Override
            protected void doExecute(DetermineDatalakeDataSizesContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(
                        context.getStackId(), DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS, "Determination of datalake data sizes in progress"
                );
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(DetermineDatalakeDataSizesContext context) {
                return new GetDatalakeDataSizesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "DETERMINE_DATALAKE_DATA_SIZES_SUCCESS_STATE")
    public Action<?, ?> determineDatalakeDataSizesSuccessAction() {
        return new AbstractDetermineDatalakeDataSizesAction<>(DetermineDatalakeDataSizesSuccessEvent.class) {
            @Override
            protected void doExecute(DetermineDatalakeDataSizesContext context, DetermineDatalakeDataSizesSuccessEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(
                        context.getStackId(), DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FINISHED, payload.getDataSizesResult()
                );
                sendEvent(context, DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SUCCESS_HANDLED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "DETERMINE_DATALAKE_DATA_SIZES_FAILURE_STATE")
    public Action<?, ?> determineDatalakeDataSizesFailureAction() {
        return new AbstractDetermineDatalakeDataSizesAction<>(StackFailureEvent.class) {
            @Override
            protected void doExecute(DetermineDatalakeDataSizesContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                getFlow(context.getFlowId()).setFlowFailed(payload.getException());
                stackUpdater.updateStackStatus(
                        context.getStackId(), DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FAILED, "Determining the datalake data sizes failed"
                );
                sendEvent(context, DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
