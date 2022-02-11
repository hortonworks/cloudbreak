package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DeregisterDatabaseServerHandler extends ExceptionCatcherEventHandler<DeregisterDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeregisterDatabaseServerHandler.class);

    private static final long DEFAULT_WORKSPACE = 0L;

    @Inject
    private EventBus eventBus;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

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
        Selectable response = new DeregisterDatabaseServerSuccess(request.getResourceId());

        DBStack dbStack = request.getDbStack();

        try {
            databaseServerConfigService.getByCrn(Crn.safeFromString(dbStack.getResourceCrn()))
                    .ifPresent(dsc -> databaseServerConfigService.delete(dsc));
            return response;
        } catch (Exception e) {
            LOGGER.warn("Error deregistering database:", e);
            return new DeregisterDatabaseServerFailed(request.getResourceId(), e);
        }
    }
}
