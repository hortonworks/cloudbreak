package com.sequenceiq.redbeams.flow.redbeams.start.actions;

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
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartState;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_START_FAILED_STATE")
public class StartDatabaseServerFailedAction extends AbstractRedbeamsFailureAction<RedbeamsStartState, RedbeamsStartEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartDatabaseServerFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Override
    protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {
        Exception failureException = payload.getException();
        LOGGER.info("Error during database server start flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.START_FAILED, errorReason);
        metricService.incrementMetricCounter(MetricType.DB_START_FAILED, dbStack);
    }

    @Override
    protected void doExecute(CommonContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new RedbeamsEvent(RedbeamsStartEvent.REDBEAMS_START_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }
}
