package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FAILED_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.CLOUD_CREDENTIAL;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.CURRENT_INSTANCE_TYPE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.DATABASE_STACK;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.RESOURCE_ID;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.TARGET_INSTANCE_TYPE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.cloudContext;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.handlerEvent;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeFailedEvent;

@ExtendWith(MockitoExtension.class)
class SyncDatabaseServerInstanceTypeHandlerTest {

    private final CloudContext cloudContext = cloudContext("AWS");

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private ResourceConnector resourceConnector;

    @InjectMocks
    private SyncDatabaseServerInstanceTypeHandler underTest;

    @Test
    void testSelector() {
        assertEquals(SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), underTest.selector());
    }

    @Test
    void testSyncReturnsLiveState() throws Exception {
        mockAuthentication();
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, false);
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenReturn(liveState);

        Selectable result = underTest.doAccept(handlerEvent(SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, null));

        ChangeInstanceTypeEvent event = assertInstanceOf(ChangeInstanceTypeEvent.class, result);
        assertEquals(CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT.event(), event.selector());
        assertEquals(RESOURCE_ID, event.getResourceId());
        assertEquals(TARGET_INSTANCE_TYPE, event.getTargetInstanceType());
        assertSame(liveState, event.getLiveState());
    }

    @Test
    void testSyncFailsWhenTheProviderLookupFails() throws Exception {
        mockAuthentication();
        Exception exception = new Exception("provider error");
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenThrow(exception);

        Selectable result = underTest.doAccept(handlerEvent(SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, null));

        ChangeInstanceTypeFailedEvent failedEvent = assertInstanceOf(ChangeInstanceTypeFailedEvent.class, result);
        assertEquals(FAILED_CHANGE_INSTANCE_TYPE_EVENT.event(), failedEvent.selector());
        assertEquals(RESOURCE_ID, failedEvent.getResourceId());
        assertSame(exception, failedEvent.getException());
    }

    private void mockAuthentication() {
        when(cloudPlatformConnectors.get(cloudContext.getPlatformVariant())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, CLOUD_CREDENTIAL)).thenReturn(ac);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }
}
