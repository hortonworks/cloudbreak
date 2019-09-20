package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DeregisterDatabaseServerHandler implements EventHandler<DeregisterDatabaseServerRequest> {

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
    public void accept(Event<DeregisterDatabaseServerRequest> event) {
        DeregisterDatabaseServerRequest request = event.getData();
        Selectable response = new DeregisterDatabaseServerSuccess(request.getResourceId());

        DBStack dbStack = request.getDbStack();

        try {
            databaseServerConfigService.getByCrn(dbStack.getResourceCrn())
                    .ifPresent(dsc -> databaseServerConfigService.delete(dsc));
            eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
        } catch (Exception e) {
            DeregisterDatabaseServerFailed failure = new DeregisterDatabaseServerFailed(request.getResourceId(), e);
            LOGGER.warn("Error deregistering database:", e);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
