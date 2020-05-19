package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopContext;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StopDatabaseServerFinishedActionTest {

    private static final long RESOURCE_ID = 123L;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private StopDatabaseServerFinishedAction victim;

    @Test
    public void shouldUpdateStatusOnPrepare() {
        StopDatabaseServerSuccess event = new StopDatabaseServerSuccess(RESOURCE_ID);

        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.STOPPED)).thenReturn(dbStack);

        victim.prepareExecution(event, null);

        verify(metricService).incrementMetricCounter(MetricType.DB_STOP_FINISHED, dbStack);
    }

    @Test
    public void shouldIncrementMetricOnCreateRequest() {
        RedbeamsStopContext context = new RedbeamsStopContext(flowParameters, cloudContext, cloudCredential, databaseStack);

        RedbeamsEvent redbeamsEvent = (RedbeamsEvent) victim.createRequest(context);

        assertEquals(RedbeamsStopEvent.REDBEAMS_STOP_FINISHED_EVENT.name(), redbeamsEvent.selector());
    }
}