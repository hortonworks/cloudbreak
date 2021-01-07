package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPO_ID_TAG;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    private static final long STACK_ID = 1L;

    private static final String AMBARI_IP = "ambariIp";

    private static final int GATEWAY_PORT = 2;

    private static final long CLUSTER_ID = 3L;

    private static final String CLUSTER_NAME = "clusterName";

    private static final String API_ADDRESS = "http://apiAddress.com";

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private StackService stackService;

    @Mock
    private AmbariClientProvider ambariClientProvider;

    @Mock
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private ClusterService clusterService;

    @Test
    void testGetStackRepositoryJsonWithWrongOsType() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setGatewayPort(GATEWAY_PORT);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setAmbariIp(AMBARI_IP);
        cluster.setName(CLUSTER_NAME);
        stack.setCluster(cluster);
        HttpClientConfig httpClientConfig = new HttpClientConfig(API_ADDRESS);
        AmbariClient ambariClient = Mockito.mock(AmbariClient.class);
        StackRepoDetails repoDetails = new StackRepoDetails();
        String stackRepoId = "stackRepoId";
        repoDetails.setStack(Map.of(REPO_ID_TAG, stackRepoId));
        String osType = "osType";

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(STACK_ID, AMBARI_IP)).thenReturn(httpClientConfig);
        when(ambariClientProvider.getAmbariClient(httpClientConfig, GATEWAY_PORT, stack.getCluster())).thenReturn(ambariClient);
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(clusterComponentConfigProvider.getStackRepoDetails(CLUSTER_ID)).thenReturn(repoDetails);
        when(ambariRepositoryVersionService.getOsTypeForStackRepoDetails(repoDetails)).thenReturn(osType);
        HttpResponseException exception = Mockito.mock(HttpResponseException.class);
        HttpResponseDecorator httpResponseDecorator = Mockito.mock(HttpResponseDecorator.class);
        when(exception.getResponse()).thenReturn(httpResponseDecorator);
        when(httpResponseDecorator.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(ambariClient.getLatestStackRepositoryAsJson(CLUSTER_NAME, osType, stackRepoId)).thenAnswer(invocation -> {
            throw exception;
        });

        assertThrows(BadRequestException.class,
                () -> clusterService.getStackRepositoryJson(STACK_ID),
                "The specified osType is wrong. ");
    }
}