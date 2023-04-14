package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesRequest;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class DeregisterServicesHandlerTest {

    private static final String TEST_DATALAKE_NAME  = "test-datalake";

    private static final String TEST_DATAHUB_NAME  = "test-datahub";

    private static final String TEST_CM_USER = "test-cm-user";

    private static final String TEST_CM_PASSWORD = "test-cm-password";

    private static final Integer TEST_GATEWAY_PORT = 8080;

    private static final Long TEST_STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackService stackService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterSecurityService clusterSecurityService;

    @Captor
    private ArgumentCaptor<Optional<DatalakeDto>> argumentCaptor;

    @InjectMocks
    private DeregisterServicesHandler underTest;

    private Event<DeregisterServicesRequest> event = new Event<>(new DeregisterServicesRequest(TEST_STACK_ID));

    @BeforeEach
    void setUp() {
        Stack datahub = new Stack();
        datahub.setName(TEST_DATAHUB_NAME);

        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(datahub);
        when(datalakeService.getDatalakeStackByDatahubStack(any(Stack.class))).thenReturn(Optional.of(getDatalakeStack()));
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(anyLong(), anyString(), anyString())).thenReturn(mock(HttpClientConfig.class));
    }

    @Test
    void testAcceptEvent() {
        when(clusterApi.clusterSecurityService()).thenReturn(clusterSecurityService);

        underTest.accept(event);

        verify(clusterSecurityService).deregisterServices(anyString(), argumentCaptor.capture());
        DatalakeDto result = argumentCaptor.getValue().get();

        assertThat(result.getName()).isEqualTo(TEST_DATALAKE_NAME);
        assertThat(result.getUser()).isEqualTo(TEST_CM_USER);
        assertThat(result.getPassword()).isEqualTo(TEST_CM_PASSWORD);
        assertThat(result.getGatewayPort()).isEqualTo(TEST_GATEWAY_PORT);
        verify(eventBus, times(1)).notify(any(), any(Event.class));
    }

    @Test
    void testAcceptEventThrowsException() {
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(anyLong(), anyString(), anyString())).thenThrow(new RuntimeException());

        underTest.accept(event);

        verify(clusterSecurityService, never()).deregisterServices(anyString(), any());
        verify(eventBus, times(1)).notify(any(), any(Event.class));
    }

    private Stack getDatalakeStack() {
        Cluster cluster = new Cluster();
        cluster.setName(TEST_DATALAKE_NAME);
        cluster.setCloudbreakClusterManagerUser(TEST_CM_USER);
        cluster.setCloudbreakClusterManagerPassword(TEST_CM_PASSWORD);
        cluster.setClusterManagerIp("test-cm-ip");

        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform("AWS");
        stack.setGatewayPort(TEST_GATEWAY_PORT);
        stack.setName(TEST_DATALAKE_NAME);
        stack.setCluster(cluster);

        return stack;
    }

}