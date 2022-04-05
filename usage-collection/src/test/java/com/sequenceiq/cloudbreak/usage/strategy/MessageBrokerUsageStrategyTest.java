package com.sequenceiq.cloudbreak.usage.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.usage.messagebroker.MessageBrokerDatabusRecordProcessor;

@ExtendWith(MockitoExtension.class)
public class MessageBrokerUsageStrategyTest {

    @InjectMocks
    private MessageBrokerUsageStrategy underTest;

    @Mock
    private MessageBrokerDatabusRecordProcessor processor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @BeforeEach
    public void setUp() {
        underTest = new MessageBrokerUsageStrategy(processor, regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void testProcessUsage() {
        // GIVEN
        doNothing().when(processor).processRecord(any());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

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
