package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.CLOUD_CREDENTIAL;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.CURRENT_INSTANCE_TYPE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.DATABASE_STACK;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.RESOURCE_ID;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.TARGET_INSTANCE_TYPE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.cloudContext;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.handlerEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.configuration.ChangeInstanceTypeConfig;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeFailedEvent;

@ExtendWith(MockitoExtension.class)
class WaitForChangeInstanceTypeHandlerTest {

    private static final Duration POLLING_INTERVAL = Duration.ofSeconds(30);

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

    @Mock
    private PollTaskFactory pollTaskFactory;

    @Mock
    private SyncPollingScheduler<DatabaseServerInstanceTypeState> syncPollingScheduler;

    @Mock
    private ChangeInstanceTypeConfig changeInstanceTypeConfig;

    @Mock
    private PollTask<DatabaseServerInstanceTypeState> pollTask;

    @InjectMocks
    private WaitForChangeInstanceTypeHandler underTest;

    @Test
    void testSelector() {
        assertEquals(WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER.event(), underTest.selector());
    }

    @ParameterizedTest
    @CsvSource({"AWS, false, 30, 60", "AWS, true, 30, 60", "AZURE, true, 30, 60", "AZURE, false, 15, 30", "GCP, true, 15, 30"})
    void testPollsWithTheProviderTimeoutAndReturnsThePolledState(String platform, boolean highAvailability, int timeoutInMinutes,
            int expectedMaxAttempts) throws Exception {
        CloudContext cloudContext = mockAuthentication(platform);
        when(changeInstanceTypeConfig.getPollingInterval()).thenReturn(POLLING_INTERVAL);
        when(changeInstanceTypeConfig.getTimeout(platform, highAvailability)).thenReturn(Duration.ofMinutes(timeoutInMinutes));
        when(pollTaskFactory.newPollChangeDatabaseServerInstanceTypeTask(ac, DATABASE_STACK, TARGET_INSTANCE_TYPE)).thenReturn(pollTask);
        DatabaseServerInstanceTypeState polledState = new DatabaseServerInstanceTypeState(TARGET_INSTANCE_TYPE, 200L, highAvailability, false);
        when(syncPollingScheduler.schedule(pollTask, 30_000, expectedMaxAttempts, SyncPollingScheduler.FAILURE_TOLERANT_ATTEMPT)).thenReturn(polledState);

        Selectable result = underTest.doAccept(handlerEvent(WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER.event(), cloudContext,
                new DatabaseServerInstanceTypeState(CURRENT_INSTANCE_TYPE, 200L, highAvailability, false)));

        ChangeInstanceTypeEvent event = assertInstanceOf(ChangeInstanceTypeEvent.class, result);
        assertEquals(CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT.event(), event.selector());
        assertEquals(RESOURCE_ID, event.getResourceId());
        assertEquals(TARGET_INSTANCE_TYPE, event.getTargetInstanceType());
        assertSame(polledState, event.getLiveState());
    }

    @Test
    void testFailsWithManualInvestigationNeededWhenThePollTimesOut() throws Exception {
        CloudContext cloudContext = mockAuthentication("AWS");
        when(changeInstanceTypeConfig.getPollingInterval()).thenReturn(POLLING_INTERVAL);
        when(changeInstanceTypeConfig.getTimeout("AWS", false)).thenReturn(Duration.ofMinutes(30));
        when(pollTaskFactory.newPollChangeDatabaseServerInstanceTypeTask(ac, DATABASE_STACK, TARGET_INSTANCE_TYPE)).thenReturn(pollTask);
        TimeoutException timeoutException = new TimeoutException("Task did not finish within 1800 seconds");
        when(syncPollingScheduler.schedule(pollTask, 30_000, 60, SyncPollingScheduler.FAILURE_TOLERANT_ATTEMPT)).thenThrow(timeoutException);

        Selectable result = underTest.doAccept(handlerEvent(WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER.event(), cloudContext,
                new DatabaseServerInstanceTypeState(CURRENT_INSTANCE_TYPE, 200L, false, true)));

        ChangeInstanceTypeFailedEvent failedEvent = assertInstanceOf(ChangeInstanceTypeFailedEvent.class, result);
        assertEquals("Changing the instance type of database server 'dbserver' to 'db.m5.xlarge' did not finish within 30 minutes. "
                + "The modification can not be cancelled on the cloud provider, manual investigation is needed.", failedEvent.getException().getMessage());
        assertSame(timeoutException, failedEvent.getException().getCause());
        assertEquals(RESOURCE_ID, failedEvent.getResourceId());
    }

    private CloudContext mockAuthentication(String platform) {
        CloudContext cloudContext = cloudContext(platform);
        when(cloudPlatformConnectors.get(cloudContext.getPlatformVariant())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, CLOUD_CREDENTIAL)).thenReturn(ac);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        return cloudContext;
    }
}
