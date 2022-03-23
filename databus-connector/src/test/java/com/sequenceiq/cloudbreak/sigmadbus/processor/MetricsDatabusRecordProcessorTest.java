package com.sequenceiq.cloudbreak.sigmadbus.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.config.SigmaDatabusConfig;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class MetricsDatabusRecordProcessorTest {

    private MetricsDatabusRecordProcessor underTest;

    @Mock
    private SigmaDatabusConfig sigmaDatabusConfig;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private RoundRobinDatabusProcessingQueues<MonitoringConfiguration> processingQueues;

    @Mock
    private Tracer tracer;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @BeforeEach
    public void setUp() {
        underTest = new MetricsDatabusRecordProcessor(sigmaDatabusConfig,
                monitoringConfiguration, 1, 1, tracer, regionAwareInternalCrnGeneratorFactory);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessRecord() throws InterruptedException {
        // GIVEN
        MetricsDatabusRecordProcessor underTestWithSpy = spy(underTest);
        doReturn(true).when(underTestWithSpy).isDatabusProcessingEnabled();
        doReturn(processingQueues).when(underTestWithSpy).getProcessingQueues();
        doNothing().when(processingQueues).process(any(DatabusRequest.class));
        // WHEN
        underTestWithSpy.processRecord(createDatabusRequest());
        // THEN
        verify(processingQueues, times(1)).process(any(DatabusRequest.class));
    }

    @Test
    public void testProcessRecordIfProcessingDisabled() throws InterruptedException {
        // GIVEN
        MetricsDatabusRecordProcessor underTestWithSpy = spy(underTest);
        doReturn(false).when(underTestWithSpy).isDatabusProcessingEnabled();
        // WHEN
        underTestWithSpy.processRecord(createDatabusRequest());
        // THEN
        verify(processingQueues, times(0)).process(any(DatabusRequest.class));
    }

    @Test
    public void testProcessRecordIfProcessingWithoutAccountId() throws InterruptedException {
        // GIVEN
        MetricsDatabusRecordProcessor underTestWithSpy = spy(underTest);
        doReturn(true).when(underTestWithSpy).isDatabusProcessingEnabled();
        // WHEN
        underTestWithSpy.processRecord(DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .build());
        // THEN
        verify(processingQueues, times(0)).process(any(DatabusRequest.class));
    }

    @Test
    public void testProcessRecordIfProcessingWithoutPayload() throws InterruptedException {
        // GIVEN
        MetricsDatabusRecordProcessor underTestWithSpy = spy(underTest);
        doReturn(true).when(underTestWithSpy).isDatabusProcessingEnabled();
        // WHEN
        underTestWithSpy.processRecord(DatabusRequest.Builder.newBuilder()
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build());
        // THEN
        verify(processingQueues, times(0)).process(any(DatabusRequest.class));
    }

    private DatabusRequest createDatabusRequest() {
        return DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build();
    }
}
