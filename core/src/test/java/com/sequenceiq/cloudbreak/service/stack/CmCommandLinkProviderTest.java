package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class CmCommandLinkProviderTest {

    private static final String CM_ADDRESS = "https://cm";

    private static final String CM_UI_SVC_NAME = "CLOUDERA_MANAGER_UI";

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @InjectMocks
    private CmCommandLinkProvider underTest;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "defaultTopologyName", "cdp-proxy");
    }

    @Test
    public void testGetLinkWhenClusterIsNull() {
        when(stackUtil.extractClusterManagerAddress(any())).thenReturn(CM_ADDRESS);
        assertTrue(underTest.getCmCommandLink(new Stack(), "111").isEmpty());
    }

    @Test
    public void testGetLinkWhenCouldNotGetCmAddress() {
        when(stackUtil.extractClusterManagerAddress(any())).thenThrow(new InternalServerErrorException("error"));
        assertTrue(underTest.getCmCommandLink(getStack(), "111").isEmpty());
    }

    @Test
    public void testGetLinkWhenDefaultTopologyCouldNotFound() {
        when(stackUtil.extractClusterManagerAddress(any())).thenReturn(CM_ADDRESS);
        when(serviceEndpointCollector.prepareClusterExposedServices(any(), any())).thenReturn(Map.of());
        assertTrue(underTest.getCmCommandLink(getStack(), "111").isEmpty());
    }

    @Test
    public void testGetLinkWhenCouldNotGetCmUiLink() {
        when(stackUtil.extractClusterManagerAddress(any())).thenReturn(CM_ADDRESS);
        when(serviceEndpointCollector.prepareClusterExposedServices(any(), any())).thenReturn(Map.of("cdp-proxy", List.of()));
        assertTrue(underTest.getCmCommandLink(getStack(), "111").isEmpty());
    }

    @Test
    public void testGetLinkCmUiLinkPresent() {
        when(stackUtil.extractClusterManagerAddress(any())).thenReturn(CM_ADDRESS);
        when(serviceEndpointCollector.prepareClusterExposedServices(any(), any())).thenReturn(Map.of("cdp-proxy", List.of(knoxServiceResponse())));
        when(exposedServiceCollector.getClouderaManagerUIService()).thenReturn(exposedService());
        Optional<String> cmCommandLink = underTest.getCmCommandLink(getStack(), "111");
        assertFalse(cmCommandLink.isEmpty());
        assertEquals("https://cm/ui/command/111/details", cmCommandLink.get());
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setCluster(new Cluster());
        return stack;
    }

    private ClusterExposedServiceV4Response knoxServiceResponse() {
        ClusterExposedServiceV4Response response = new ClusterExposedServiceV4Response();
        response.setServiceName(CM_UI_SVC_NAME);
        response.setServiceUrl(CM_ADDRESS + "/ui/home/");
        return response;
    }

    private ExposedService exposedService() {
        ExposedService service = new ExposedService();
        service.setServiceName(CM_UI_SVC_NAME);
        return service;
    }

}
