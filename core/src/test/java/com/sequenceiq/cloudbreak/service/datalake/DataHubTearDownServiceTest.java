package com.sequenceiq.cloudbreak.service.datalake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class DataHubTearDownServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:default:cluster:0ba0ca99-e961-4c8d-b7e9-da0587cd40d0";

    private static final String TEST_DATALAKE_NAME  = "test-datalake";

    private static final String TEST_DH_NAME  = "test-datahub";

    private static final String TEST_CM_USER = "test-cm-user";

    private static final String TEST_CM_PASSWORD = "test-cm-password";

    private static final Integer TEST_GATEWAY_PORT = 8080;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackService stackService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterSecurityService clusterSecurityService;

    @Captor
    private ArgumentCaptor<Optional<DatalakeDto>> argumentCaptor;

    @InjectMocks
    private DataHubTearDownService underTest;

    @BeforeEach
    void setUp() {
        Stack datahub = new Stack();
        datahub.setResourceCrn(DATAHUB_CRN);
        datahub.setName(TEST_DH_NAME);

        when(stackService.getByCrnWithLists(anyString())).thenReturn(datahub);
        when(stackService.getByCrnOrElseNull(anyString())).thenReturn(getDatalakeStack());
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(anyLong(), anyString(), anyString())).thenReturn(new HttpClientConfig(null));
    }

    @Test
    void testAcceptEvent() {
        when(clusterApi.clusterSecurityService()).thenReturn(clusterSecurityService);

        underTest.tearDownDataHub(DATALAKE_CRN, DATAHUB_CRN);

        verify(clusterSecurityService).deregisterServices(anyString(), argumentCaptor.capture());
        DatalakeDto result = argumentCaptor.getValue().get();

        assertEquals(result.getName(), TEST_DATALAKE_NAME);
        assertEquals(result.getUser(), TEST_CM_USER);
        assertEquals(result.getPassword(), TEST_CM_PASSWORD);
        assertEquals(result.getGatewayPort(), TEST_GATEWAY_PORT);
    }

    @Test
    void testAcceptEventThrowsException() {
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(anyLong(), anyString(), anyString())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> underTest.tearDownDataHub(DATALAKE_CRN, DATAHUB_CRN));

        verify(clusterSecurityService, never()).deregisterServices(anyString(), any());
    }

    private Stack getDatalakeStack() {
        Cluster cluster = new Cluster();
        cluster.setName(TEST_DATALAKE_NAME);
        cluster.setCloudbreakAmbariUser(TEST_CM_USER);
        cluster.setCloudbreakAmbariPassword(TEST_CM_PASSWORD);
        cluster.setClusterManagerIp("test-cm-ip");

        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform("AWS");
        stack.setGatewayPort(TEST_GATEWAY_PORT);
        stack.setName(TEST_DATALAKE_NAME);
        stack.setResourceCrn(DATALAKE_CRN);
        stack.setCluster(cluster);

        return stack;
    }

}