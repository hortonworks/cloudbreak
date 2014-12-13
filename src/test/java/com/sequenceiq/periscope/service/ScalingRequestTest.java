package com.sequenceiq.periscope.service;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@RunWith(MockitoJUnitRunner.class)
public class ScalingRequestTest {

    private static final int S_RETRY_COUNT = 10;
    private static final int C_RETRY_COUNT = 3;
    private static final int SLEEP = 0;

    private ScalingRequest scalingRequest;
    @Mock
    private CloudbreakService cloudbreakService;

    @Test
    public void testWaitForReadyStateForTimeout() throws Exception {
        Cluster cluster = mock(Cluster.class);
        ScalingPolicy scalingPolicy = mock(ScalingPolicy.class);
        CloudbreakClient cloudbreakClient = mock(CloudbreakClient.class);
        when(scalingPolicy.getHostGroup()).thenReturn("group");
        when(cluster.getHost()).thenReturn("ambari.com");
        when(cluster.getId()).thenReturn(1L);
        when(cloudbreakClient.resolveToStackId("ambari.com")).thenReturn(50);
        when(cloudbreakClient.getStackStatus(50)).thenReturn("UPDATING");
        when(cloudbreakService.getClient()).thenReturn(cloudbreakClient);
        scalingRequest = new ScalingRequest(cluster, scalingPolicy, 5, 10, SLEEP, S_RETRY_COUNT, C_RETRY_COUNT);
        ReflectionTestUtils.setField(scalingRequest, "cloudbreakService", cloudbreakService);

        scalingRequest.run();

        verify(cloudbreakClient, times(1)).putStack(50, 5);
        verify(cloudbreakClient, times(10)).getStackStatus(50);
        verify(cloudbreakClient, times(0)).putCluster(50, "group", 5);
    }

    @Test
    public void testWaitForReadyStateForAvailable() throws Exception {
        Cluster cluster = mock(Cluster.class);
        ScalingPolicy scalingPolicy = mock(ScalingPolicy.class);
        CloudbreakClient cloudbreakClient = mock(CloudbreakClient.class);
        when(scalingPolicy.getHostGroup()).thenReturn("group");
        when(cluster.getHost()).thenReturn("ambari.com");
        when(cluster.getId()).thenReturn(1L);
        when(cloudbreakClient.resolveToStackId("ambari.com")).thenReturn(50);
        when(cloudbreakClient.getStackStatus(50)).thenReturn("AVAILABLE");
        when(cloudbreakService.getClient()).thenReturn(cloudbreakClient);
        scalingRequest = new ScalingRequest(cluster, scalingPolicy, 5, 10, SLEEP, S_RETRY_COUNT, C_RETRY_COUNT);
        ReflectionTestUtils.setField(scalingRequest, "cloudbreakService", cloudbreakService);

        scalingRequest.run();

        verify(cloudbreakClient, times(1)).putStack(50, 5);
        verify(cloudbreakClient, times(2)).getStackStatus(50);
        verify(cloudbreakClient, times(1)).putCluster(50, "group", 5);
    }

    @Test
    public void testWaitForReadyStateForAvailableSecondTime() throws Exception {
        Cluster cluster = mock(Cluster.class);
        ScalingPolicy scalingPolicy = mock(ScalingPolicy.class);
        CloudbreakClient cloudbreakClient = mock(CloudbreakClient.class);
        when(scalingPolicy.getHostGroup()).thenReturn("group");
        when(cluster.getHost()).thenReturn("ambari.com");
        when(cluster.getId()).thenReturn(1L);
        when(cloudbreakClient.resolveToStackId("ambari.com")).thenReturn(50);
        when(cloudbreakClient.getStackStatus(50)).thenAnswer(new Answer<String>() {
            private int invocation;

            @Override
            public String answer(InvocationOnMock mock) throws Exception {
                switch (invocation++) {
                    case 0:
                    case 2:
                        return "AVAILABLE";
                    default:
                        return "UPDATING";
                }
            }
        });
        when(cloudbreakService.getClient()).thenReturn(cloudbreakClient);
        scalingRequest = new ScalingRequest(cluster, scalingPolicy, 5, 10, SLEEP, S_RETRY_COUNT, C_RETRY_COUNT);
        ReflectionTestUtils.setField(scalingRequest, "cloudbreakService", cloudbreakService);

        scalingRequest.run();

        verify(cloudbreakClient, times(1)).putStack(50, 5);
        verify(cloudbreakClient, times(3)).getStackStatus(50);
        verify(cloudbreakClient, times(1)).putCluster(50, "group", 5);
    }

    @Test
    public void testWaitForReadyStateForSingleAvailableState() throws Exception {
        Cluster cluster = mock(Cluster.class);
        ScalingPolicy scalingPolicy = mock(ScalingPolicy.class);
        CloudbreakClient cloudbreakClient = mock(CloudbreakClient.class);
        when(scalingPolicy.getHostGroup()).thenReturn("group");
        when(cluster.getHost()).thenReturn("ambari.com");
        when(cluster.getId()).thenReturn(1L);
        when(cloudbreakClient.resolveToStackId("ambari.com")).thenReturn(50);
        when(cloudbreakClient.getStackStatus(50)).thenAnswer(new Answer<String>() {
            private int invocation;

            @Override
            public String answer(InvocationOnMock mock) throws Exception {
                switch (invocation++) {
                    case 1:
                        return "AVAILABLE";
                    default:
                        return "UPDATING";
                }
            }
        });
        when(cloudbreakService.getClient()).thenReturn(cloudbreakClient);
        scalingRequest = new ScalingRequest(cluster, scalingPolicy, 5, 10, SLEEP, S_RETRY_COUNT, C_RETRY_COUNT);
        ReflectionTestUtils.setField(scalingRequest, "cloudbreakService", cloudbreakService);

        scalingRequest.run();

        verify(cloudbreakClient, times(1)).putStack(50, 5);
        verify(cloudbreakClient, times(11)).getStackStatus(50);
        verify(cloudbreakClient, times(0)).putCluster(50, "group", 5);
    }

    @Test
    public void testSendInstallRequestFail() throws Exception {
        Cluster cluster = mock(Cluster.class);
        ScalingPolicy scalingPolicy = mock(ScalingPolicy.class);
        CloudbreakClient cloudbreakClient = mock(CloudbreakClient.class);
        when(scalingPolicy.getHostGroup()).thenReturn("group");
        when(cluster.getHost()).thenReturn("ambari.com");
        when(cluster.getId()).thenReturn(1L);
        when(cloudbreakClient.resolveToStackId("ambari.com")).thenReturn(50);
        when(cloudbreakClient.getStackStatus(50)).thenReturn("AVAILABLE");
        doThrow(new RuntimeException()).when(cloudbreakClient).putCluster(50, "group", 5);
        when(cloudbreakService.getClient()).thenReturn(cloudbreakClient);
        scalingRequest = new ScalingRequest(cluster, scalingPolicy, 5, 10, SLEEP, S_RETRY_COUNT, C_RETRY_COUNT);
        ReflectionTestUtils.setField(scalingRequest, "cloudbreakService", cloudbreakService);

        scalingRequest.run();

        verify(cloudbreakClient, times(1)).putStack(50, 5);
        verify(cloudbreakClient, times(2)).getStackStatus(50);
        verify(cloudbreakClient, times(3)).putCluster(50, "group", 5);
    }

    @Test
    public void testSendInstallRequestForSecondTry() throws Exception {
        Cluster cluster = mock(Cluster.class);
        ScalingPolicy scalingPolicy = mock(ScalingPolicy.class);
        CloudbreakClient cloudbreakClient = mock(CloudbreakClient.class);
        when(scalingPolicy.getHostGroup()).thenReturn("group");
        when(cluster.getHost()).thenReturn("ambari.com");
        when(cluster.getId()).thenReturn(1L);
        when(cloudbreakClient.resolveToStackId("ambari.com")).thenReturn(50);
        when(cloudbreakClient.getStackStatus(50)).thenReturn("AVAILABLE");
        doAnswer(new Answer<String>() {
            private int invocation;

            @Override
            public String answer(InvocationOnMock mock) throws Exception {
                switch (invocation++) {
                    case 0:
                        throw new RuntimeException();
                    default:
                        return "AVAILABLE";
                }
            }
        }).when(cloudbreakClient).putCluster(50, "group", 5);
        when(cloudbreakService.getClient()).thenReturn(cloudbreakClient);
        scalingRequest = new ScalingRequest(cluster, scalingPolicy, 5, 10, SLEEP, S_RETRY_COUNT, C_RETRY_COUNT);
        ReflectionTestUtils.setField(scalingRequest, "cloudbreakService", cloudbreakService);

        scalingRequest.run();

        verify(cloudbreakClient, times(1)).putStack(50, 5);
        verify(cloudbreakClient, times(2)).getStackStatus(50);
        verify(cloudbreakClient, times(2)).putCluster(50, "group", 5);
    }

}
