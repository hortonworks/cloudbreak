package com.sequenceiq.periscope.service.configuration;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;

@RunWith(MockitoJUnitRunner.class)
public class AmbariConfigurationServiceTest {

    @Mock
    private AmbariClient ambariClient;

    @Test
    public void testResolveInternalAddress() throws ConnectException {
        Map<String, Map<String, String>> serviceConfig = new HashMap<>();
        String rm = ConfigParam.YARN_RM_ADDRESS.key();
        serviceConfig.put("yarn", singletonMap(rm, "ec2.internal.address:8050"));
        when(ambariClient.getServiceConfigMap()).thenReturn(serviceConfig);
        when(ambariClient.resolveInternalHostName("ec2.internal.address")).thenReturn("ec2.public.address");

        Configuration configuration = AmbariConfigurationService.getConfiguration(1, ambariClient);

        assertEquals(configuration.get(rm), "ec2.public.address:8050");
    }

    @Test
    public void testAzureInternalAddressResolution() throws ConnectException {
        Map<String, Map<String, String>> serviceConfig = new HashMap<>();
        String rm = ConfigParam.YARN_RM_ADDRESS.key();
        serviceConfig.put("yarn", singletonMap(rm, "azure-address.internal.cloudapp.net:8050"));
        when(ambariClient.getServiceConfigMap()).thenReturn(serviceConfig);
        when(ambariClient.resolveInternalHostName("azure-address.internal.cloudapp.net"))
                .thenReturn("azure-address.internal.cloudapp.net");

        Configuration configuration = AmbariConfigurationService.getConfiguration(1, ambariClient);

        assertEquals(configuration.get(rm), "azure-address.cloudapp.net:8050");
    }
}
