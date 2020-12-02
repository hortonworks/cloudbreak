package com.sequenceiq.cloudbreak.ccmimpl.termination;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCcmV2AgentTerminationListenerTest {

    @InjectMocks
    private DefaultCcmV2AgentTerminationListener underTest;

    @Mock
    private CcmV2ManagementClient ccmV2Client;

    @Test
    public void testDeregisterInvertingProxyAgent() {
        String testAgentCrn = "testAgentCrn";
        underTest.deregisterInvertingProxyAgent(testAgentCrn);
        verify(ccmV2Client, times(1)).deregisterInvertingProxyAgent(anyString(), eq(testAgentCrn));
    }

    @Test
    public void testDeregisterInvertingProxyAgentWhenCcmVNotConfigured() {
        underTest.deregisterInvertingProxyAgent(null);
        verify(ccmV2Client, never()).deregisterInvertingProxyAgent(anyString(), anyString());
    }
}
