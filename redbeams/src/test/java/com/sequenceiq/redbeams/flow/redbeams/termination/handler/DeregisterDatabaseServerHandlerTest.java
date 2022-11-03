package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class DeregisterDatabaseServerHandlerTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:tenantId:databaseServer:dbId";

    @Mock
    private EventBus eventBus;

    @Mock
    private DatabaseServerConfigService databaseServerConfigService;

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private DeregisterDatabaseServerHandler underTest;

    @Mock
    private Event<DeregisterDatabaseServerRequest> event;

    @Test
    public void testDeleteDatabaseServerConfigIfExists() {
        mockDeregisterEvent();

        DBStack dbStack = new DBStack();
        dbStack.setResourceCrn(RESOURCE_CRN);
        when(dbStackService.getById(RESOURCE_ID)).thenReturn(dbStack);

        DatabaseServerConfig databaseServerConfig = new DatabaseServerConfig();
        when(databaseServerConfigService.getByCrn(any(Crn.class))).thenReturn(Optional.of(databaseServerConfig));

        underTest.accept(event);

        verify(databaseServerConfigService).delete(databaseServerConfig);
        verify(eventBus).notify(any(), any(Event.class));
    }

    @Test
    public void testNotDeleteDatabaseServerConfigIfExists() {
        mockDeregisterEvent();

        DBStack dbStack = new DBStack();
        dbStack.setResourceCrn(RESOURCE_CRN);
        when(dbStackService.getById(RESOURCE_ID)).thenReturn(dbStack);

        when(databaseServerConfigService.getByCrn(any(Crn.class))).thenReturn(Optional.empty());

        underTest.accept(event);

        verify(databaseServerConfigService, times(0)).delete(any());
        verify(eventBus).notify(any(), any(Event.class));
    }

    private void mockDeregisterEvent() {
        when(event.getData()).thenReturn(new DeregisterDatabaseServerRequest(new CloudContext.Builder().withId(RESOURCE_ID).build(), null));
        when(event.getHeaders()).thenReturn(new Event.Headers());
    }
}