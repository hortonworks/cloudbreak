package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component("REDBEAMS_START_FAILED_STATE")
public class StartDatabaseServerFailedAction extends AbstractRedbeamsStartAction<RedbeamsFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartDatabaseServerFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public StartDatabaseServerFailedAction() {
        super(RedbeamsFailureEvent.class);
    }

    @Override
    protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {
        Exception failureException = payload.getException();
        LOGGER.info("Error during database server start flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        DBStack dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.START_FAILED, errorReason);
        metricService.incrementMetricCounter(MetricType.DB_START_FAILED, dbStack);
    }

    @Override
    protected RedbeamsStartContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsStartState, RedbeamsStartEvent> stateContext,
            RedbeamsFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());

        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected Selectable createRequest(RedbeamsStartContext context) {
        return new RedbeamsEvent(RedbeamsStartEvent.REDBEAMS_START_FAILURE_HANDLED_EVENT.event(), 0L);
    }
}
