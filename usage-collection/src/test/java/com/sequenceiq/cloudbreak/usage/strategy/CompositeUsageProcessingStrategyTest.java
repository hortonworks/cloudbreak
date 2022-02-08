package com.sequenceiq.cloudbreak.usage.strategy;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.messagebroker.MessageBrokerConfiguration;
import com.sequenceiq.cloudbreak.usage.http.EdhHttpConfiguration;

@ExtendWith(MockitoExtension.class)
public class CompositeUsageProcessingStrategyTest {

    @InjectMocks
    private CompositeUsageProcessingStrategy underTest;

    @Mock
    private HttpUsageProcessingStrategy httpUsageProcessingStrategy;

    @Mock
    private LoggingUsageProcessingStrategy loggingUsageProcessingStrategy;

    @Mock
    private MessageBrokerUsageStrategy messageBrokerUsageStrategy;

    @Mock
    private EdhHttpConfiguration edhHttpConfiguration;

    @Mock
    private MessageBrokerConfiguration messageBrokerConfiguration;

    @BeforeEach
    public void setUp() {
        underTest = new CompositeUsageProcessingStrategy(loggingUsageProcessingStrategy, httpUsageProcessingStrategy, messageBrokerUsageStrategy,
                edhHttpConfiguration, messageBrokerConfiguration);
    }

    @Test
    public void testProcessUsageDefaultLogging() {
        // GIVEN
        // WHEN
        underTest.init();
        underTest.processUsage(null, null);
        // THEN
        verify(loggingUsageProcessingStrategy, times(1)).processUsage(null, null);
        verify(httpUsageProcessingStrategy, times(0)).processUsage(null, null);
        verify(messageBrokerUsageStrategy, times(0)).processUsage(null, null);
    }

    @Test
    public void testProcessUsageWithForcedLogging() {
        // GIVEN
        given(edhHttpConfiguration.isEnabled()).willReturn(true);
        given(edhHttpConfiguration.isForceLogging()).willReturn(true);
        // WHEN
        underTest.init();
        underTest.processUsage(null, null);
        // THEN
        verify(edhHttpConfiguration, times(2)).isEnabled();
        verify(edhHttpConfiguration, times(1)).isForceLogging();
        verify(loggingUsageProcessingStrategy, times(1)).processUsage(null, null);
        verify(httpUsageProcessingStrategy, times(1)).processUsage(null, null);
        verify(messageBrokerUsageStrategy, times(0)).processUsage(null, null);
    }

    @Test
    public void testProcessUsageWithBothHttpAndMessageBroker() {
        // GIVEN
        given(edhHttpConfiguration.isEnabled()).willReturn(true);
        given(messageBrokerConfiguration.isEnabled()).willReturn(true);
        // WHEN
        underTest.init();
        underTest.processUsage(null, null);
        // THEN
        verify(edhHttpConfiguration, times(2)).isEnabled();
        verify(loggingUsageProcessingStrategy, times(0)).processUsage(null, null);
        verify(httpUsageProcessingStrategy, times(1)).processUsage(null, null);
        verify(messageBrokerUsageStrategy, times(1)).processUsage(null, null);
    }
}
