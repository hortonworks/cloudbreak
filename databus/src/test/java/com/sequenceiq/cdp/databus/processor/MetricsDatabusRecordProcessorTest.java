package com.sequenceiq.cdp.databus.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

import com.sequenceiq.cdp.databus.model.DatabusRecordInput;
import com.sequenceiq.cdp.databus.model.DatabusRequestContext;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cdp.databus.tracing.DatabusClientTracingFeatureFactory;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

@ExtendWith(MockitoExtension.class)
public class MetricsDatabusRecordProcessorTest {

    private MetricsDatabusRecordProcessor underTest;

    @Mock
    private AccountDatabusConfigService accountDatabusConfigService;

    @Mock
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private DatabusClientTracingFeatureFactory databusClientTracingFeatureFactory;

    @Mock
    private RoundRobinDatabusProcessingQueues<MonitoringConfiguration> processingQueues;

    @BeforeEach
    public void setUp() {
        underTest = new MetricsDatabusRecordProcessor(accountDatabusConfigService, altusDatabusConfiguration,
                monitoringConfiguration, 1, 1);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessRecord() throws InterruptedException {
        // GIVEN
        String json = "{\\n\"name\": \"value\"\\n}";
        DatabusRequestContext context = new DatabusRequestContext("cloudera", null, null, null);
        context.addAdditionalDatabusHeader("header-1", "header-1-value")
                .addAdditionalDatabusHeader("header-2", "header-2-value");
        MetricsDatabusRecordProcessor underTestWithSpy = spy(underTest);
        given(monitoringConfiguration.getDbusAppNameKey()).willReturn("app-key");
        doReturn(monitoringConfiguration).when(underTestWithSpy).getDatabusStreamConfiguration();
        doReturn(true).when(underTestWithSpy).isDatabusProcessingEnabled();
        doReturn(processingQueues).when(underTestWithSpy).getProcessingQueues();
        doNothing().when(processingQueues).process(any(DatabusRecordInput.class));
        // WHEN
        underTestWithSpy.processRecord(json, context);
        // THEN
        verify(processingQueues, times(1)).process(any(DatabusRecordInput.class));
    }

    @Test
    public void testProcessRecordIfProcessingDisabled() throws InterruptedException {
        // GIVEN
        MetricsDatabusRecordProcessor underTestWithSpy = spy(underTest);
        doReturn(false).when(underTestWithSpy).isDatabusProcessingEnabled();
        // WHEN
        underTestWithSpy.processRecord("{}", new DatabusRequestContext("cloudera", null, null, null));
        // THEN
        verify(processingQueues, times(0)).process(any(DatabusRecordInput.class));
    }
}
