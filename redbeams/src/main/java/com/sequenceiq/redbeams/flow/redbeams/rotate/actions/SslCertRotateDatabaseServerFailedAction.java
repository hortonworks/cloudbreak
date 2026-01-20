package com.sequenceiq.redbeams.flow.redbeams.rotate.actions;

import static com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus.SSL_ROTATE_FAILED;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.metrics.MetricType.DB_ROTATE_CERT_FAILED;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_SSL_CERT_ROTATE_FAILED_STATE")
public class SslCertRotateDatabaseServerFailedAction extends AbstractRedbeamsSslCertRotateAction<SslCertRotateDatabaseRedbeamsFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslCertRotateDatabaseServerFailedAction.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public SslCertRotateDatabaseServerFailedAction() {
        super(SslCertRotateDatabaseRedbeamsFailureEvent.class);
    }

    @Override
    protected void prepareExecution(SslCertRotateDatabaseRedbeamsFailureEvent payload, Map<Object, Object> variables) {
        Exception failureException = payload.getException();
        LOGGER.warn("Error during database server rotate cert flow:", failureException);

        String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(
                payload.getResourceId(),
                SSL_ROTATE_FAILED,
                errorReason);
        metricService.incrementMetricCounter(
                DB_ROTATE_CERT_FAILED,
                dbStack);
    }

    @Override
    protected SslCertRotateDatabaseRedbeamsContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent> stateContext,
            SslCertRotateDatabaseRedbeamsFailureEvent payload) {
        return super.createFlowContext(flowParameters, stateContext, payload, false);
    }

    @Override
    protected Object getFailurePayload(
            SslCertRotateDatabaseRedbeamsFailureEvent payload,
            Optional<SslCertRotateDatabaseRedbeamsContext> flowContext,
            Exception ex) {
        return new SslCertRotateDatabaseRedbeamsFailureEvent(payload.getResourceId(), ex, payload.isOnlyCertificateUpdate());
    }

    @Override
    protected Selectable createRequest(SslCertRotateDatabaseRedbeamsContext context) {
        return new SslCertRotateRedbeamsEvent(
                REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT.event(),
                context.getDBStack().getId(),
                context.isOnlyCertificateUpdate());
    }
}
