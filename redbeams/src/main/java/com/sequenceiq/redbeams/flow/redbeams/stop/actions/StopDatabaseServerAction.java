package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopContext;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component("STOP_DATABASE_SERVER_STATE")
public class StopDatabaseServerAction extends AbstractRedbeamsStopAction<RedbeamsEvent> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    public StopDatabaseServerAction() {
        super(RedbeamsEvent.class);
    }

    @Override
    protected void prepareExecution(RedbeamsEvent payload, Map<Object, Object> variables) {
        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.STOP_IN_PROGRESS);
    }

    @Override
    protected Selectable createRequest(RedbeamsStopContext context) {
        return new StopDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(),
                context.getDbInstanceIdentifier());
    }
}
