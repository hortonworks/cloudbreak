package com.sequenceiq.cloudbreak.usage.strategy;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloudwatch.model.CloudwatchRecordRequest;
import com.sequenceiq.cloudbreak.usage.processor.EdhCloudwatchConfiguration;
import com.sequenceiq.cloudbreak.usage.processor.EdhCloudwatchProcessor;

@ExtendWith(MockitoExtension.class)
public class CloudwatchUsageProcessingStrategyTest {

    private CloudwatchUsageProcessingStrategy underTest;

    @Mock
    private EdhCloudwatchProcessor edhCloudwatchProcessor;

    @Mock
    private EdhCloudwatchConfiguration edhCloudwatchConfiguration;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new CloudwatchUsageProcessingStrategy(edhCloudwatchProcessor, edhCloudwatchConfiguration);
    }

    @Test
    public void testProcessUsage() {
        // GIVEN
        UsageProto.CDPEnvironmentRequested details = UsageProto.CDPEnvironmentRequested
                .newBuilder().build();
        UsageProto.Event event = UsageProto.Event.newBuilder()
                .setCdpEnvironmentRequested(details)
                .build();
        // WHEN
        underTest.processUsage(event);
        underTest.processUsage(event);
        // THEN
        verify(edhCloudwatchProcessor, times(2)).processRecord(any(CloudwatchRecordRequest.class));
    }
}
