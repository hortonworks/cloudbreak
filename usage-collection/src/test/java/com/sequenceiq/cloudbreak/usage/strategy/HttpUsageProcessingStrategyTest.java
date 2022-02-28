package com.sequenceiq.cloudbreak.usage.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.http.UsageHttpRecordProcessor;

@ExtendWith(MockitoExtension.class)
public class HttpUsageProcessingStrategyTest {

    @InjectMocks
    private HttpUsageProcessingStrategy underTest;

    @Mock
    private UsageHttpRecordProcessor processor;

    @BeforeEach
    public void setUp() {
        underTest = new HttpUsageProcessingStrategy(processor);
    }

    @Test
    public void testProcessUsage() {
        // GIVEN
        doNothing().when(processor).processRecord(any());
        UsageProto.Event event = UsageProto.Event.newBuilder()
                .setCdpEnvironmentRequested(UsageProto.CDPEnvironmentRequested
                        .newBuilder()
                        .build())
                .build();
        // WHEN
        underTest.processUsage(event, null);
        // THEN
        verify(processor, times(1)).processRecord(any());
    }
}
