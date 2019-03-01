package com.sequenceiq.cloudbreak.ambari;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClientFactoryTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @InjectMocks
    private final AmbariClientFactory underTest = new AmbariClientFactory();

    @Test
    public void testGetDefaultAmbariClientWhenEverythingWorksFine() {
        Stack stack = TestUtil.stack();
        HttpClientConfig httpClientConfig = new HttpClientConfig(stack.getAmbariIp());
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClientProvider.getDefaultAmbariClient(httpClientConfig, stack.getGatewayPort())).thenReturn(ambariClient);

        AmbariClient defaultAmbariClient = underTest.getDefaultAmbariClient(stack, httpClientConfig);

        Assert.assertEquals(ambariClient, defaultAmbariClient);

        verify(ambariClientProvider, times(1)).getDefaultAmbariClient(httpClientConfig, stack.getGatewayPort());
    }

    @Test
    public void testGetAmbariClientWhenEverythingWorksFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        HttpClientConfig httpClientConfig = new HttpClientConfig(stack.getAmbariIp());
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClientProvider.getAmbariClient(httpClientConfig, stack.getGatewayPort(), cluster)).thenReturn(ambariClient);

        AmbariClient defaultAmbariClient = underTest.getAmbariClient(stack, cluster, httpClientConfig);

        Assert.assertEquals(ambariClient, defaultAmbariClient);

        verify(ambariClientProvider, times(1)).getAmbariClient(httpClientConfig, stack.getGatewayPort(), cluster);
    }

    @Test
    public void testGetAmbariClientWithUsernameAndPasswordWhenEverythingWorksFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();
        stack.setCluster(cluster);
        String userName = "userName";
        String password = "password";
        HttpClientConfig httpClientConfig = new HttpClientConfig(stack.getAmbariIp());
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);

        when(ambariClientProvider.getAmbariClient(httpClientConfig, stack.getGatewayPort(), userName, password)).thenReturn(ambariClient);

        AmbariClient defaultAmbariClient = underTest.getAmbariClient(stack, userName, password, httpClientConfig);

        Assert.assertEquals(ambariClient, defaultAmbariClient);

        verify(ambariClientProvider, times(1)).getAmbariClient(httpClientConfig, stack.getGatewayPort(), userName, password);
    }

}