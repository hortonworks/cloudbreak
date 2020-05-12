package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;
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
public class StartDatabaseServerFinishedActionTest {

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
    private DBStack dbStack;

    @Mock
    private DatabaseStack databaseStack;

    @InjectMocks
    private StartDatabaseServerFinishedAction victim;

    @Test
    public void shouldUpdateStatusOnPrepare() {
        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.STARTED)).thenReturn(dbStack);

        StartDatabaseServerSuccess event = new StartDatabaseServerSuccess(RESOURCE_ID);

        victim.prepareExecution(event, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.STARTED);
        verify(metricService).incrementMetricCounter(MetricType.DB_START_FINISHED, dbStack);
    }

    @Test
    public void shouldIncrementMetricOnCreateRequest() {
        RedbeamsStartContext context = new RedbeamsStartContext(flowParameters, cloudContext, cloudCredential, databaseStack);

        RedbeamsEvent redbeamsEvent = (RedbeamsEvent) victim.createRequest(context);

        assertEquals(RedbeamsStartEvent.REDBEAMS_START_FINISHED_EVENT.name(), redbeamsEvent.selector());
    }
}