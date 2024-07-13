package com.sequenceiq.redbeams.flow.redbeams.rotate.actions;

import static com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus.SSL_ROTATE_IN_PROGRESS;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.rotate.SslCertRotateDatabaseRedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("SSL_CERT_ROTATE_DATABASE_SERVER_STATE")
public class SslCertRotateDatabaseServerAction extends AbstractRedbeamsSslCertRotateAction<SslCertRotateRedbeamsEvent> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    public SslCertRotateDatabaseServerAction() {
        super(SslCertRotateRedbeamsEvent.class);
    }

    @Override
    protected void prepareExecution(SslCertRotateRedbeamsEvent payload, Map<Object, Object> variables) {
        dbStackStatusUpdater.updateStatus(payload.getResourceId(), SSL_ROTATE_IN_PROGRESS);
    }

    @Override
    protected Selectable createRequest(SslCertRotateDatabaseRedbeamsContext context) {
        return new SslCertRotateDatabaseServerRequest(
                context.getCloudContext(),
                context.getCloudCredential(),
                context.getDatabaseStack(),
                context.isOnlyCertificateUpdate());
    }

    @Override
    protected SslCertRotateDatabaseRedbeamsContext createFlowContext(
            FlowParameters flowParameters,
            StateContext<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent> stateContext,
            SslCertRotateRedbeamsEvent payload) {
        return super.createFlowContext(flowParameters, stateContext, payload, payload.isOnlyCertificateUpdate());
    }

    @Override
    protected Object getFailurePayload(SslCertRotateRedbeamsEvent payload, Optional<SslCertRotateDatabaseRedbeamsContext> flowContext, Exception ex) {
        return new SslCertRotateDatabaseRedbeamsFailureEvent(payload.getResourceId(), ex, payload.isOnlyCertificateUpdate());
    }
}
