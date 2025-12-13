package com.sequenceiq.cloudbreak.ccmimpl.termination;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2RetryingClient;

@ExtendWith(MockitoExtension.class)
class DefaultCcmV2AgentTerminationListenerTest {

    @InjectMocks
    private DefaultCcmV2AgentTerminationListener underTest;

    @Mock
    private CcmV2RetryingClient ccmV2Client;

    @Test
    void testDeregisterInvertingProxyAgent() {
        String testAgentCrn = "testAgentCrn";
        underTest.deregisterInvertingProxyAgent(testAgentCrn);
        verify(ccmV2Client, times(1)).deregisterInvertingProxyAgent(eq(testAgentCrn));
    }

    @Test
    void testDeregisterInvertingProxyAgentWhenCcmVNotConfigured() {
        underTest.deregisterInvertingProxyAgent(null);
        verify(ccmV2Client, never()).deregisterInvertingProxyAgent(anyString());
    }
}
