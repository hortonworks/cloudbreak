package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.BatchResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchRequestElement;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.config.ClouderaManagerFlinkConfigurationService;
import com.sequenceiq.cloudbreak.cm.config.modification.ClouderaManagerConfigModificationService;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerUpdateTrustedRealmsTest {

    private static final String STACK_NAME = "stack_name";

    @InjectMocks
    private ClouderaManagerModificationService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient v31Client;

    @Spy
    private Stack stack;

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClouderaManagerClientConfigDeployService clouderaManagerClientConfigDeployService;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private BatchResourceApi batchResourceApi;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ClouderaManagerRoleRefreshService clouderaManagerRoleRefreshService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClouderaManagerConfigService configService;

    @Mock
    private ClouderaManagerParcelDecommissionService clouderaManagerParcelDecommissionService;

    @Mock
    private ClouderaManagerParcelManagementService clouderaManagerParcelManagementService;

    @Mock
    private ClouderaManagerUpgradeService clouderaManagerUpgradeService;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    private ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerRestartService clouderaManagerRestartService;

    @Mock
    private ClouderaManagerConfigModificationService clouderaManagerConfigModificationService;

    @Mock
    private ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    @Mock
    private ClusterCommandService clusterCommandService;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Spy
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private ClouderaManagerFlinkConfigurationService clouderaManagerFlinkConfigurationService;

    @Mock
    private ClouderaManagerKraftMigrationService clouderaManagerKraftMigrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        stack.setName(STACK_NAME);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName(STACK_NAME);
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
    }

    @Test
    public void testUpdateTrustedRealmsCallsApiWithUpperCasedRealm() throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(v31Client)).thenReturn(batchResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(configService.getServiceName(STACK_NAME, "core_settings", servicesResourceApi)).thenReturn(Optional.of("core_settings-1"));
        TrustView trustView = new TrustView("10.0.0.1", "kdc.example.com", "example.com");

        underTest.updateTrustedRealms(trustView);

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        ApiBatchRequest batchRequest = batchRequestCaptor.getValue();
        assertThat(batchRequest.getItems()).hasSize(1);
        ApiBatchRequestElement element = batchRequest.getItems().get(0);
        assertEquals(HTTPMethod.PUT, element.getMethod());
        assertEquals(ClouderaManagerApiClientProvider.API_V_31 + "/clusters/" + STACK_NAME + "/services/core_settings-1/config", element.getUrl());
        assertThat(element.getBody()).isInstanceOf(ApiServiceConfig.class);
        ApiServiceConfig serviceConfig = (ApiServiceConfig) element.getBody();
        assertThat(serviceConfig.getItems()).hasSize(1);
        ApiConfig config = serviceConfig.getItems().get(0);
        assertEquals("trusted_realms", config.getName());
        assertEquals("EXAMPLE.COM", config.getValue());
    }

    @Test
    public void testUpdateTrustedRealmsThrowsCloudManagerOperationFailedExceptionOnApiException() throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(v31Client)).thenReturn(batchResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(configService.getServiceName(STACK_NAME, "core_settings", servicesResourceApi)).thenReturn(Optional.of("core_settings-1"));
        when(batchResourceApi.execute(any(ApiBatchRequest.class))).thenThrow(new ApiException("CM API failure"));
        TrustView trustView = new TrustView("10.0.0.1", "kdc.example.com", "EXAMPLE.COM");

        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.updateTrustedRealms(trustView));

        assertThat(exception.getMessage()).contains("CM API failure");
    }
}

