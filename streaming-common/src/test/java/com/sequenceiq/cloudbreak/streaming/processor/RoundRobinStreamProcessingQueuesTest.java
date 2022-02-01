package com.sequenceiq.cloudbreak.streaming.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

@ExtendWith(MockitoExtension.class)
public class RoundRobinStreamProcessingQueuesTest {

    @Mock
    private AbstractRecordProcessor recordProcessor;

    @Mock
    private AbstractStreamingConfiguration streamingConfiguration;

    @Mock
    private RecordWorker recordWorker;

    @Test
    public void testProcess() throws InterruptedException {
        // GIVEN
        MockitoAnnotations.openMocks(this);
        DummyRequest input = new DummyRequest("body", null, new Date().getTime(), true);
        given(recordProcessor.getServiceName()).willReturn("DummyService");
        doNothing().when(recordProcessor).handleDroppedRecordRequest(input, 2);
        RoundRobinStreamProcessingQueues<AbstractStreamingConfiguration, RecordRequest, RecordWorker> underTest =
                new RoundRobinStreamProcessingQueues<>(2, 2, recordProcessor);
        // WHEN
        underTest.process(input);
        underTest.process(input);
        underTest.process(input);
        underTest.process(input);
        underTest.process(input);
        // THEN
        assertEquals(2, underTest.getProcessingQueueList().get(0).size());
        assertEquals(2, underTest.getProcessingQueueList().get(1).size());
        verify(recordProcessor, times(1)).handleDroppedRecordRequest(input, 2);
    }

    static class DummyRequest extends RecordRequest {

        DummyRequest(String rawBody, GeneratedMessageV3 messageBody, long timestamp, boolean forceRawOutput) {
            super(rawBody, messageBody, timestamp, forceRawOutput);
        }
    }
}
