package com.sequenceiq.cloudbreak.reactor.handler.cluster.deregister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class DeregisterPrePositionFactoryTest {

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private DeregisterPrePositionFactory deregisterPrePositionFactory;

    private StackDtoDelegate stackDtoDelegate;

    private StackView stackView;

    private Stack stack;

    private HttpClientConfig httpClientConfig;

    @BeforeEach
    public void setUp() {
        stackDtoDelegate = mock(StackDtoDelegate.class);
        stackView = mock(StackView.class);
        stack = mock(Stack.class);
        httpClientConfig = mock(HttpClientConfig.class);
    }

    @Test
    public void testClusterApi() {
        ClusterApi expectedClusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(stackDtoDelegate)).thenReturn(expectedClusterApi);

        ClusterApi actualClusterApi = deregisterPrePositionFactory.clusterApi(stackDtoDelegate);

        assertEquals(expectedClusterApi, actualClusterApi);
        verify(clusterApiConnectors).getConnector(stackDtoDelegate);
    }

    @Test
    public void testDatalakeDto() {
        // Mock the datalakeService to return an Optional with a Stack
        when(stackService.getByCrnOrElseNull(any())).thenReturn(stack);

        // Mock the stack methods
        when(stack.getId()).thenReturn(1L);
        when(stack.getClusterManagerIp()).thenReturn("clusterManagerIp");
        when(stack.cloudPlatform()).thenReturn("cloudPlatform");
        when(stack.getGatewayPort()).thenReturn(8080);
        when(stack.getName()).thenReturn("stackName");
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);

        // Mock the stack's cluster
        Cluster cluster = mock(Cluster.class);
        when(cluster.getCloudbreakClusterManagerUser()).thenReturn("user");
        when(cluster.getCloudbreakClusterManagerPassword()).thenReturn("password");
        when(stack.getCluster()).thenReturn(cluster);

        // Mock the tlsSecurityService to return an HttpClientConfig
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(any(), any(), any())).thenReturn(httpClientConfig);

        Optional<DatalakeDto> datalakeDtoOptional = deregisterPrePositionFactory.datalakeDto(stackView);

        assertTrue(datalakeDtoOptional.isPresent());
        DatalakeDto datalakeDto = datalakeDtoOptional.get();
        assertEquals("stackName", datalakeDto.getName());
        assertEquals(Status.AVAILABLE, datalakeDto.getStatus());
        assertEquals(8080, datalakeDto.getGatewayPort().intValue());
        assertEquals(httpClientConfig, datalakeDto.getHttpClientConfig());
        assertEquals("user", datalakeDto.getUser());
        assertEquals("password", datalakeDto.getPassword());

        verify(stackService).getByCrnOrElseNull(any());
        verify(tlsSecurityService).buildTLSClientConfigForPrimaryGateway(1L, "clusterManagerIp", "cloudPlatform");
    }

    @Test
    public void testDatalakeDtoEmpty() {
        when(stackService.getByCrnOrElseNull(any())).thenReturn(null);

        Optional<DatalakeDto> datalakeDtoOptional = deregisterPrePositionFactory.datalakeDto(stackView);

        assertFalse(datalakeDtoOptional.isPresent());
        verify(stackService).getByCrnOrElseNull(any());
    }
}