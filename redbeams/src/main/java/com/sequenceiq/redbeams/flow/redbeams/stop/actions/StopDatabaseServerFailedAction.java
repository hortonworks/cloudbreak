package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_STOP_FAILED_STATE")
public class StopDatabaseServerFailedAction extends AbstractRedbeamsFailureAction<RedbeamsStopState, RedbeamsStopEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopDatabaseServerFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Override
    protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {
        Exception failureException = payload.getException();
        LOGGER.info("Error during database server stop flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.STOP_FAILED, errorReason);
        metricService.incrementMetricCounter(MetricType.DB_STOP_FAILED, dbStack);
    }

    @Override
    protected void doExecute(CommonContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new RedbeamsEvent(RedbeamsStopEvent.REDBEAMS_STOP_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }
}
