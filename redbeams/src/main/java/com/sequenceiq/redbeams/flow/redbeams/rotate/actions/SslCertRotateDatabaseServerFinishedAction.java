package com.sequenceiq.redbeams.flow.redbeams.rotate.actions;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
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
        metricService.incrementMetricCounter(MetricType.DB_ROTATE_CERT_FINISHED, dbStack);
    }

    @Override
    protected Selectable createRequest(RedbeamsContext context) {
        return new RedbeamsEvent(RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FINISHED_EVENT.name(), context.getDBStack().getId());
    }
}
