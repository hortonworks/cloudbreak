package com.sequenceiq.cloudbreak.ccmimpl.termination;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CRN;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2ManagementClient;
import com.sequenceiq.cloudbreak.common.json.Json;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCcmV2AgentTerminationListenerTest {

    @InjectMocks
    private DefaultCcmV2AgentTerminationListener underTest;

    @Mock
    private CcmV2ManagementClient ccmV2Client;

    @Test
    public void testDeregisterInvertingProxyAgent() {
        String testAgentCrn = "testAgentCrn";
        Json ccmV2Config = mock(Json.class);
        when(ccmV2Config.getSilent(Map.class)).thenReturn(Map.of(CCMV2_AGENT_CRN, testAgentCrn));

        underTest.deregisterInvertingProxyAgent(ccmV2Config);

        verify(ccmV2Client, times(1)).deregisterInvertingProxyAgent(anyString(), eq(testAgentCrn));
    }

    @Test
    public void testDeregisterInvertingProxyAgentWhenCcmVNotConfigured() {
        underTest.deregisterInvertingProxyAgent(null);
        verify(ccmV2Client, never()).deregisterInvertingProxyAgent(anyString(), anyString());
    }
}
