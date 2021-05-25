package com.sequenceiq.cdp.databus.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cdp.databus.client.DatabusClient;
import com.sequenceiq.cdp.databus.model.DatabusRecordInput;
import com.sequenceiq.cdp.databus.model.DatabusRequestContext;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

@ExtendWith(MockitoExtension.class)
public class RoundRobinDatabusProcessingQueuesTest {

    @Mock
    private DatabusClient.Builder clientBuilder;

    @Mock
    private AccountDatabusConfigService accountDatabusConfigService;

    @Mock
    private MetricsDatabusRecordProcessor metricsDatabusRecordProcessor;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Test
    public void testProcess() throws InterruptedException {
        // GIVEN
        MockitoAnnotations.openMocks(this);
        DatabusRecordInput.Builder builder = DatabusRecordInput.Builder.builder()
                .withPayload("{}")
                .withDatabusRequestContext(new DatabusRequestContext("cloudera", null, null, null))
                .withDatabusStreamConfiguration(monitoringConfiguration);
        given(metricsDatabusRecordProcessor.getDatabusStreamConfiguration()).willReturn(monitoringConfiguration);
        given(monitoringConfiguration.getDbusServiceName()).willReturn("Monitoring");
        given(monitoringConfiguration.getDbusStreamName()).willReturn("CdpVmMetrics");
        RoundRobinDatabusProcessingQueues<MonitoringConfiguration> underTest =
                new RoundRobinDatabusProcessingQueues<>(2, 2, clientBuilder, accountDatabusConfigService, metricsDatabusRecordProcessor);
        // WHEN
        underTest.process(builder.build());
        underTest.process(builder.build());
        underTest.process(builder.build());
        underTest.process(builder.build());
        underTest.process(builder.build());
        // THEN
        assertEquals(2, underTest.getProcessingQueueList().get(0).size());
        assertEquals(2, underTest.getProcessingQueueList().get(1).size());
    }

}
