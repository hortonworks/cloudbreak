package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopContext;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component("REDBEAMS_STOP_FAILED_STATE")
public class StopDatabaseServerFailedAction extends AbstractRedbeamsStopAction<RedbeamsFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopDatabaseServerFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public StopDatabaseServerFailedAction() {
        super(RedbeamsFailureEvent.class);
    }

    @Override
    protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {
        Exception failureException = payload.getException();
        LOGGER.info("Error during database server stop flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        DBStack dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.STOP_FAILED, errorReason);
        metricService.incrementMetricCounter(MetricType.DB_STOP_FAILED, dbStack);
    }

    @Override
    protected RedbeamsStopContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsStopState,
            RedbeamsStopEvent> stateContext,
            RedbeamsFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());

        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected Selectable createRequest(RedbeamsStopContext context) {
        return new RedbeamsEvent(RedbeamsStopEvent.REDBEAMS_STOP_FAILURE_HANDLED_EVENT.event(), 0L);
    }
}
