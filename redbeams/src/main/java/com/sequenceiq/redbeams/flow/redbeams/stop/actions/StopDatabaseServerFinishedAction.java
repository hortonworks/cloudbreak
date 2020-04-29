package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopContext;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricTag;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component("REDBEAMS_STOP_FINISHED_STATE")
public class StopDatabaseServerFinishedAction extends AbstractRedbeamsStopAction<StopDatabaseServerSuccess> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public StopDatabaseServerFinishedAction() {
        super(StopDatabaseServerSuccess.class);
    }

    @Override
    protected void prepareExecution(StopDatabaseServerSuccess payload, Map<Object, Object> variables) {
        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.STOPPED);
    }

    @Override
    protected Selectable createRequest(RedbeamsStopContext context) {
        metricService.incrementMetricCounter(MetricType.DB_STOP_FINISHED, RedbeamsMetricTag.DATABASE_VENDOR.name(), context.getDbVendorDisplayName());

        return new RedbeamsEvent(RedbeamsStopEvent.REDBEAMS_STOP_FINISHED_EVENT.name(), 0L);
    }
}
