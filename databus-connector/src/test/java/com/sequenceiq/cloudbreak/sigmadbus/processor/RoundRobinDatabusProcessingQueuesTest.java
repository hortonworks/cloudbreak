package com.sequenceiq.cloudbreak.sigmadbus.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class RoundRobinDatabusProcessingQueuesTest {

    @Mock
    private AbstractDatabusRecordProcessor recordProcessor;

    @Mock
    private AbstractDatabusStreamConfiguration databusStreamConfiguration;

    @Mock
    private Tracer tracer;

    @Test
    public void testProcess() throws InterruptedException {
        // GIVEN
        MockitoAnnotations.openMocks(this);
        DatabusRequest input = DatabusRequest.Builder.newBuilder().build();
        given(recordProcessor.getDatabusStreamConfiguration()).willReturn(databusStreamConfiguration);
        given(databusStreamConfiguration.getDbusServiceName()).willReturn("Monitoring");
        doNothing().when(recordProcessor).handleDroppedDatabusRequest(input, 2);
        RoundRobinDatabusProcessingQueues<AbstractDatabusStreamConfiguration> underTest =
                new RoundRobinDatabusProcessingQueues<>(2, 2, recordProcessor, tracer, null);
        // WHEN
        underTest.process(input);
        underTest.process(input);
        underTest.process(input);
        underTest.process(input);
        underTest.process(input);
        // THEN
        assertEquals(2, underTest.getProcessingQueueList().get(0).size());
        assertEquals(2, underTest.getProcessingQueueList().get(1).size());
        verify(recordProcessor, times(1)).handleDroppedDatabusRequest(input, 2);
    }

}
