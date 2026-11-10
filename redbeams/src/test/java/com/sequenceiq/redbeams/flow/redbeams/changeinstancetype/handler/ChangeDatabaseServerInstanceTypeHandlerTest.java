package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class ChangeDatabaseServerInstanceTypeHandlerTest {

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
    private ChangeDatabaseServerInstanceTypeHandler underTest;

    @Test
    void testSelector() {
        assertEquals(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), underTest.selector());
    }

    @Test
    void testModificationIsIssuedWhenTheTargetIsNotApplied() throws Exception {
        mockAuthentication();
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, false);
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenReturn(liveState);

        Selectable result = underTest.doAccept(handlerEvent(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, liveState));

        verify(resourceConnector).changeDatabaseServerInstanceType(ac, DATABASE_STACK, TARGET_INSTANCE_TYPE);
        assertModifyFinished(result, liveState);
    }

    @Test
    void testModificationIsSkippedWhenTheTargetIsAlreadyApplied() throws Exception {
        mockAuthentication();
        DatabaseServerInstanceTypeState liveState = state(TARGET_INSTANCE_TYPE, true);
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenReturn(liveState);

        Selectable result = underTest.doAccept(handlerEvent(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, liveState));

        verify(resourceConnector, never()).changeDatabaseServerInstanceType(any(), any(), any());
        assertModifyFinished(result, liveState);
    }

    @Test
    void testModificationIsSkippedWhenTheTargetIsAlreadyAppliedIgnoringCase() throws Exception {
        mockAuthentication();
        DatabaseServerInstanceTypeState liveState = state(TARGET_INSTANCE_TYPE.toUpperCase(), false);
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenReturn(liveState);

        Selectable result = underTest.doAccept(handlerEvent(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, liveState));

        verify(resourceConnector, never()).changeDatabaseServerInstanceType(any(), any(), any());
        assertModifyFinished(result, liveState);
    }

    @Test
    void testModificationIsSkippedWhenAModificationIsInProgress() throws Exception {
        mockAuthentication();
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, true);
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenReturn(liveState);

        Selectable result = underTest.doAccept(handlerEvent(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, liveState));

        verify(resourceConnector, never()).changeDatabaseServerInstanceType(any(), any(), any());
        assertModifyFinished(result, liveState);
    }

    @Test
    void testFailsWhenTheProviderRejectsTheModification() throws Exception {
        mockAuthentication();
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, false);
        when(resourceConnector.getDatabaseServerInstanceTypeState(ac, DATABASE_STACK)).thenReturn(liveState);
        Exception exception = new Exception("InvalidParameterCombination");
        doThrow(exception).when(resourceConnector).changeDatabaseServerInstanceType(ac, DATABASE_STACK, TARGET_INSTANCE_TYPE);

        Selectable result = underTest.doAccept(handlerEvent(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER.event(), cloudContext, liveState));

        ChangeInstanceTypeFailedEvent failedEvent = assertInstanceOf(ChangeInstanceTypeFailedEvent.class, result);
        assertSame(exception, failedEvent.getException());
    }

    private void assertModifyFinished(Selectable result, DatabaseServerInstanceTypeState liveState) {
        ChangeInstanceTypeEvent event = assertInstanceOf(ChangeInstanceTypeEvent.class, result);
        assertEquals(CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT.event(), event.selector());
        assertEquals(RESOURCE_ID, event.getResourceId());
        assertEquals(TARGET_INSTANCE_TYPE, event.getTargetInstanceType());
        assertSame(liveState, event.getLiveState());
    }

    private void mockAuthentication() {
        when(cloudPlatformConnectors.get(cloudContext.getPlatformVariant())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, CLOUD_CREDENTIAL)).thenReturn(ac);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }
}
