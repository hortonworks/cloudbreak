package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Component("REDBEAMS_START_FINISHED_STATE")
public class StartDatabaseServerFinishedAction extends AbstractRedbeamsStartAction<StartDatabaseServerSuccess> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    public StartDatabaseServerFinishedAction() {
        super(StartDatabaseServerSuccess.class);
    }

    @Override
    protected void prepareExecution(StartDatabaseServerSuccess payload, Map<Object, Object> variables) {
        Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.STARTED);
        metricService.incrementMetricCounter(MetricType.DB_START_FINISHED, dbStack);
    }

    @Override
    protected Selectable createRequest(RedbeamsStartContext context) {
        return new RedbeamsEvent(RedbeamsStartEvent.REDBEAMS_START_FINISHED_EVENT.name(), 0L);
    }
}
