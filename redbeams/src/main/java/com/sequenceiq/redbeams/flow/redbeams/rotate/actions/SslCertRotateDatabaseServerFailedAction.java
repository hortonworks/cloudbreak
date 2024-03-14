package com.sequenceiq.redbeams.flow.redbeams.rotate.actions;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_SSL_CERT_ROTATE_FAILED_STATE")
public class SslCertRotateDatabaseServerFailedAction extends AbstractRedbeamsSslCertRotateAction<RedbeamsFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslCertRotateDatabaseServerFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public SslCertRotateDatabaseServerFailedAction() {
        super(RedbeamsFailureEvent.class);
    }

    @Override
    protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {
        Exception failureException = payload.getException();
        LOGGER.warn("Error during database server rotate cert flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.SSL_ROTATE_FAILED, errorReason);
        metricService.incrementMetricCounter(MetricType.DB_ROTATE_CERT_FAILED, dbStack);
    }

    @Override
    protected RedbeamsContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsSslCertRotateState,
            RedbeamsSslCertRotateEvent> stateContext,
            RedbeamsFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());

        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected Selectable createRequest(RedbeamsContext context) {
        return new RedbeamsEvent(RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT.event(), context.getDBStack().getId());
    }
}
