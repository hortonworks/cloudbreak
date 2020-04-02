package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component("START_DATABASE_SERVER_STATE")
public class StartDatabaseServerAction extends AbstractRedbeamsStartAction<RedbeamsEvent> {

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    public StartDatabaseServerAction() {
        super(RedbeamsEvent.class);
    }

    @Override
    protected void prepareExecution(RedbeamsEvent payload, Map<Object, Object> variables) {
        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.START_IN_PROGRESS);
    }

    @Override
    protected Selectable createRequest(RedbeamsStartContext context) {
        return new StartDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(),
            context.getDbInstanceIdentifier());
    }
}
