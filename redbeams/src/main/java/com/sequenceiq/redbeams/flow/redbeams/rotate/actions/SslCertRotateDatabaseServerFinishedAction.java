package com.sequenceiq.redbeams.flow.redbeams.rotate.actions;

import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FINISHED_EVENT;
import static com.sequenceiq.redbeams.metrics.MetricType.DB_ROTATE_CERT_FINISHED;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_SSL_CERT_ROTATE_FINISHED_STATE")
public class SslCertRotateDatabaseServerFinishedAction extends AbstractRedbeamsSslCertRotateAction<SslCertRotateDatabaseServerSuccess> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public SslCertRotateDatabaseServerFinishedAction() {
        super(SslCertRotateDatabaseServerSuccess.class);
    }

    @Override
    protected void prepareExecution(SslCertRotateDatabaseServerSuccess payload, Map<Object, Object> variables) {
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.SSL_ROTATED);
        metricService.incrementMetricCounter(DB_ROTATE_CERT_FINISHED, dbStack);
    }

    @Override
    protected Selectable createRequest(SslCertRotateDatabaseRedbeamsContext context) {
        return new SslCertRotateRedbeamsEvent(
                REDBEAMS_SSL_CERT_ROTATE_FINISHED_EVENT.name(),
                context.getDBStack().getId(),
                context.isOnlyCertificateUpdate());
    }

    @Override
    protected SslCertRotateDatabaseRedbeamsContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent> stateContext,
            SslCertRotateDatabaseServerSuccess payload) {
        return super.createFlowContext(flowParameters, stateContext, payload, payload.isOnlyCertificateUpdate());
    }

    @Override
    protected Object getFailurePayload(SslCertRotateDatabaseServerSuccess payload, Optional<SslCertRotateDatabaseRedbeamsContext> flowContext, Exception ex) {
        return new SslCertRotateDatabaseRedbeamsFailureEvent(payload.getResourceId(), ex, payload.isOnlyCertificateUpdate());
    }
}
