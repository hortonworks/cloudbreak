package com.sequenceiq.redbeams.flow.redbeams.rotate.actions;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("SSL_CERT_ROTATE_DATABASE_SERVER_STATE")
public class SslCertRotateDatabaseServerAction extends AbstractRedbeamsSslCertRotateAction<RedbeamsEvent> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    public SslCertRotateDatabaseServerAction() {
        super(RedbeamsEvent.class);
    }

    @Override
    protected void prepareExecution(RedbeamsEvent payload, Map<Object, Object> variables) {
        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.SSL_ROTATE_IN_PROGRESS);
    }

    @Override
    protected Selectable createRequest(RedbeamsContext context) {
        return new SslCertRotateDatabaseServerRequest(
                context.getCloudContext(),
                context.getCloudCredential(),
                context.getDatabaseStack());
    }
}
