package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfigureForKerberosArguments;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.type.KerberosType;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerKerberosServiceTest {

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ClouderaManagerModificationService modificationService;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerClusterDecommissionService decommissionService;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private ApiClient client;

    @Mock
    private ClouderaManagerConfigService clouderaManagerConfigService;

    @Mock
    private ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    @Mock
    private ClouderaManagerClientConfigDeployService clouderaManagerClientConfigDeployService;

    @InjectMocks
    private ClouderaManagerKerberosService underTest;

    private Stack stack;

    private Cluster cluster;

    private HttpClientConfig clientConfig;

    @BeforeEach
    void init() throws ClouderaManagerClientInitException {
        stack = new Stack();
        stack.setName("clusterName");
        stack.setGatewayPort(1);
        cluster = new Cluster();
        cluster.setName("clusterName");
        cluster.setCloudbreakClusterManagerUser("user");
        cluster.setCloudbreakClusterManagerPassword("password");
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        clientConfig = new HttpClientConfig("1.2.3.4", null, null, null);
        lenient().when(clouderaManagerApiClientProvider.getV31Client(anyInt(), anyString(), anyString(), any())).thenReturn(client);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(client)).thenReturn(clouderaManagerResourceApi);
        when(applicationContext.getBean(eq(ClouderaManagerModificationService.class), eq(stack), eq(clientConfig))).thenReturn(modificationService);
        lenient().when(applicationContext.getBean(eq(ClouderaManagerClusterDecommissionService.class), eq(stack), eq(clientConfig)))
                .thenReturn(decommissionService);
    }

    @Test
    void testConfigureKerberosViaApi() throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withType(KerberosType.ACTIVE_DIRECTORY)
                .withRealm("TESTREALM")
                .withUrl("kdcs")
                .withAdminUrl("adminhosts")
                .withContainerDn("container")
                .withPrincipal("principal")
                .withPassword("pw")
                .build();

        when(clouderaManagerApiFactory.getClustersResourceApi(client)).thenReturn(clustersResourceApi);
        when(clustersResourceApi.configureForKerberos(eq(cluster.getName()), any(ApiConfigureForKerberosArguments.class)))
                .thenReturn(new ApiCommand().id(BigDecimal.TEN));
        when(clouderaManagerResourceApi.generateCredentialsCommand()).thenReturn(new ApiCommand().id(BigDecimal.ZERO));
        when(kerberosDetailService.isAdJoinable(kerberosConfig)).thenReturn(Boolean.TRUE);

        underTest.configureKerberosViaApi(client, clientConfig, stack, kerberosConfig);

        verify(modificationService).stopCluster(false);
        verify(clouderaManagerPollingServiceProvider).startPollingCmKerberosJob(stack, client, BigDecimal.TEN);
        verify(clouderaManagerPollingServiceProvider).startPollingCmKerberosJob(stack, client, BigDecimal.ZERO);
        verify(clouderaManagerClientConfigDeployService).deployAndPollClientConfig(any());
        verify(modificationService).startCluster();
    }

    @Test
    void deleteCredentials() throws ApiException, CloudbreakException {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(client)).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.deleteCredentialsCommand("all")).thenReturn(new ApiCommand().id(BigDecimal.ZERO));

        underTest.deleteCredentials(clientConfig, stack);

        verify(modificationService).stopCluster(false);
        clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, BigDecimal.ZERO);
    }
}
