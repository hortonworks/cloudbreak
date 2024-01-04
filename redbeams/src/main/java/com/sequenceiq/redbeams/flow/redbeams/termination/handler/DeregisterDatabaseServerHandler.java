package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class DeregisterDatabaseServerHandler extends ExceptionCatcherEventHandler<DeregisterDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeregisterDatabaseServerHandler.class);

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private DBStackService dbStackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeregisterDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeregisterDatabaseServerRequest> event) {
        return new DeregisterDatabaseServerFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DeregisterDatabaseServerRequest> event) {
        DeregisterDatabaseServerRequest request = event.getData();

        DBStack dbStack = dbStackService.getById(request.getResourceId());

        try {
            databaseServerConfigService.getByCrn(Crn.safeFromString(dbStack.getResourceCrn()))
                    .ifPresent(dsc -> databaseServerConfigService.delete(dsc));
            return new DeregisterDatabaseServerSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.warn("Error deregistering database:", e);
            return new DeregisterDatabaseServerFailed(request.getResourceId(), e);
        }
    }
}
