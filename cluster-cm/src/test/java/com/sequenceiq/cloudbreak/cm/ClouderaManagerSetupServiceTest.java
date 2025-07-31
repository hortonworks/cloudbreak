package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerSetupService.POLICY_DESCRIPTION;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerSetupService.POLICY_DESCRIPTION_GOV;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerSetupService.POLICY_NAME;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerSetupService.POLICY_NAME_GOV;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerSetupService.POLICY_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.CdpResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiConfigPolicy;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.error.mapper.ClouderaManagerStorageErrorMapper;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerSetupServiceTest {

    private static final String PROXY_PROTOCOL = "tcp";

    private static final String PROXY_HOST = "10.0.0.0";

    private static final int PROXY_PORT = 88;

    private static final String PROXY_USER = "user";

    private static final String PROXY_PASSWORD = "pw";

    private static final String PROXY_NO_PROXY_HOSTS = "noproxy.com";

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClouderaHostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Mock
    private CentralCmTemplateUpdater cmTemplateUpdater;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ClouderaManagerLicenseService clouderaManagerLicenseService;

    @Mock
    private ClouderaManagerMgmtSetupService mgmtSetupService;

    @Mock
    private ClouderaManagerKerberosService kerberosService;

    @Mock
    private ClouderaManagerMgmtLaunchService clouderaManagerMgmtLaunchService;

    @Mock
    private ClouderaManagerSupportSetupService clouderaManagerSupportSetupService;

    @Mock
    private ClouderaManagerYarnSetupService clouderaManagerYarnSetupService;

    @Mock
    private ClusterCommandService clusterCommandService;

    @Mock
    private ClouderaManagerStorageErrorMapper clouderaManagerStorageErrorMapper;

    @Mock
    private ClouderaManagerCipherService clouderaManagerCipherService;

    @Mock
    private ClouderaManagerFedRAMPService clouderaManagerFedRAMPService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @InjectMocks
    private ClouderaManagerSetupService underTest;

    @BeforeEach
    void before() {
        underTest = new ClouderaManagerSetupService(testStack(), new HttpClientConfig("10.0.0.0"));

        ReflectionTestUtils.setField(underTest, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(underTest, "clouderaManagerPollingServiceProvider", clouderaManagerPollingServiceProvider);
        ReflectionTestUtils.setField(underTest, "hostGroupAssociationBuilder", hostGroupAssociationBuilder);
        ReflectionTestUtils.setField(underTest, "cmTemplateUpdater", cmTemplateUpdater);
        ReflectionTestUtils.setField(underTest, "clusterComponentProvider", clusterComponentProvider);
        ReflectionTestUtils.setField(underTest, "clouderaManagerLicenseService", clouderaManagerLicenseService);
        ReflectionTestUtils.setField(underTest, "mgmtSetupService", mgmtSetupService);
        ReflectionTestUtils.setField(underTest, "kerberosService", kerberosService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerMgmtLaunchService", clouderaManagerMgmtLaunchService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerSupportSetupService", clouderaManagerSupportSetupService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerYarnSetupService", clouderaManagerYarnSetupService);
        ReflectionTestUtils.setField(underTest, "clusterCommandService", clusterCommandService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerCipherService", clouderaManagerCipherService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerFedRAMPService", clouderaManagerFedRAMPService);
        ReflectionTestUtils.setField(underTest, "apiClient", mock(ApiClient.class));
        ReflectionTestUtils.setField(underTest, "blueprintUtils", blueprintUtils);
        ReflectionTestUtils.setField(underTest, "entitlementService", entitlementService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerCommandsService", clouderaManagerCommandsService);
    }

    @Test
    void testAutoconfigureWhenItDoesItsJob() throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = mock(MgmtServiceResourceApi.class);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any(ApiClient.class)))
                .thenReturn(mgmtServiceResourceApi);

        underTest.autoConfigureClusterManager();

        verify(mgmtServiceResourceApi, times(1)).autoConfigure();
    }

    @Test
    void testAutoconfigureWhenThrowsException() throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = mock(MgmtServiceResourceApi.class);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any(ApiClient.class)))
                .thenReturn(mgmtServiceResourceApi);
        doThrow(ApiException.class).when(mgmtServiceResourceApi).autoConfigure();

        Assertions.assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.autoConfigureClusterManager());
        verify(mgmtServiceResourceApi, times(1)).autoConfigure();
    }

    @Test
    void testValidateLicenceWhenEverythingWorkAsExpectedShouldNotThrowException() {
        doNothing().when(clouderaManagerLicenseService).validateClouderaManagerLicense(any());

        underTest.validateLicence();

        verify(clouderaManagerLicenseService, times(1)).validateClouderaManagerLicense(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testPublishPolicyWhenCMVersionHigherThan792(boolean govCloud) throws ApiException {
        ArgumentCaptor<ApiConfigPolicy> argumentCaptor = ArgumentCaptor.forClass(ApiConfigPolicy.class);
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);

        underTest.publishPolicy("{\"cdhVersion\":\"7.2.16\",\"cmVersion\":\"7.9.2\",\"displayName\":\"opdb\"}", govCloud);

        verify(clouderaManagerResourceApi, times(1)).addConfigPolicy(argumentCaptor.capture());
        verify(clouderaManagerCipherService, times(1)).getApiConfigEnforcements();
        verify(clouderaManagerFedRAMPService, times(govCloud ? 1 : 0)).getApiConfigEnforcements();
        ApiConfigPolicy apiConfigPolicy = argumentCaptor.getValue();
        assertEquals(apiConfigPolicy.getVersion(), POLICY_VERSION);
        assertEquals(apiConfigPolicy.getName(), govCloud ? POLICY_NAME_GOV : POLICY_NAME);
        assertEquals(apiConfigPolicy.getDescription(), govCloud ? POLICY_DESCRIPTION_GOV : POLICY_DESCRIPTION);
    }

    @Test
    void testPublishPolicyWhenCMVersionLowerThan792() throws ApiException, IOException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        underTest.publishPolicy("{\"cdhVersion\":\"7.2.16\",\"cmVersion\":\"7.9.1\",\"displayName\":\"opdb\"}", true);

        verify(clouderaManagerCipherService, never()).getApiConfigEnforcements();
        verify(clouderaManagerFedRAMPService, never()).getApiConfigEnforcements();
        verify(clouderaManagerResourceApi, never()).addConfigPolicy(any());
    }

    @Test
    void testValidateLicenceWhenItThrowExceptionShouldMapToClouderaManagerOperationFailedException() {
        doThrow(new RuntimeException()).when(clouderaManagerLicenseService).validateClouderaManagerLicense(any());

        ClouderaManagerOperationFailedException actual =
                assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.validateLicence());

        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
        verify(clouderaManagerLicenseService, times(1)).validateClouderaManagerLicense(any());
    }

    @Test
    void testConfigureManagementServicesWhenApiExceptionHappensThenShouldThrowClouderaManagerOperationFailedException() throws Exception {
        ApiException error = mock(ApiException.class);
        HostsResourceApi mockHostsResourceApi = mock(HostsResourceApi.class);
        TemplatePreparationObject mockTemplatePreparationObject = mock(TemplatePreparationObject.class);
        String mockSdxContext = JsonUtil.writeValueAsString(new ApiRemoteDataContext());
        String mockSdxStackCrn = "mockSdxStackCrn";
        Telemetry telemetry = mock(Telemetry.class);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        ApiClient apiClient = mock(ApiClient.class);
        ApiHostList apiHostList = mock(ApiHostList.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        CdpResourceApi cdpResourceApi = mock(CdpResourceApi.class);
        ApiRemoteDataContext mockApiRemoteDataContext = mock(ApiRemoteDataContext.class);

        when(error.getResponseBody()).thenReturn(null);
        when(error.getMessage()).thenReturn("error");
        when(mockHostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()))
                .thenReturn(apiHostList);
        when(mockTemplatePreparationObject.getGeneralClusterConfigs())
                .thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN())
                .thenReturn(Optional.of("fqdn"));
        when(apiHostList.getItems()).thenReturn(List.of(apiHost("fqdn")));
        when(clouderaManagerApiClientProvider.getRootClient(any(Integer.class), anyString(), anyString(), any(HttpClientConfig.class)))
                .thenReturn(apiClient);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(mockHostsResourceApi);
        when(clouderaManagerApiFactory.getCdpResourceApi(any(ApiClient.class))).thenReturn(cdpResourceApi);
        when(cdpResourceApi.postRemoteContext(any(ApiRemoteDataContext.class))).thenReturn(mockApiRemoteDataContext);
        when(mockApiRemoteDataContext.getEndPointId()).thenReturn("endpoint");
        doThrow(error).when(mgmtSetupService).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );

        ClouderaManagerOperationFailedException actual =
                assertThrows(ClouderaManagerOperationFailedException.class,
                        () -> underTest.configureManagementServices(mockTemplatePreparationObject, mockSdxContext, mockSdxStackCrn, telemetry, proxyConfig));

        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
        verify(mgmtSetupService, times(1)).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );
    }

    @Test
    void testConfigureManagementServicesWhenCManagerOperationFailedExceptionHappensThenShouldThrowCManagerOperationFailedException() throws Exception {
        HostsResourceApi mockHostsResourceApi = mock(HostsResourceApi.class);
        TemplatePreparationObject mockTemplatePreparationObject = mock(TemplatePreparationObject.class);
        String mockSdxContext = JsonUtil.writeValueAsString(new ApiRemoteDataContext());
        String mockSdxStackCrn = "mockSdxStackCrn";
        Telemetry telemetry = mock(Telemetry.class);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        ApiClient apiClient = mock(ApiClient.class);
        ApiHostList apiHostList = mock(ApiHostList.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        CdpResourceApi cdpResourceApi = mock(CdpResourceApi.class);
        ApiRemoteDataContext mockApiRemoteDataContext = mock(ApiRemoteDataContext.class);

        when(mockHostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()))
                .thenReturn(apiHostList);
        when(mockTemplatePreparationObject.getGeneralClusterConfigs())
                .thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN())
                .thenReturn(Optional.of("fqdn"));
        when(apiHostList.getItems()).thenReturn(List.of(apiHost("fqdn")));
        when(clouderaManagerApiClientProvider.getRootClient(any(Integer.class), anyString(), anyString(), any(HttpClientConfig.class)))
                .thenReturn(apiClient);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(mockHostsResourceApi);
        when(clouderaManagerApiFactory.getCdpResourceApi(any(ApiClient.class))).thenReturn(cdpResourceApi);
        when(cdpResourceApi.postRemoteContext(any(ApiRemoteDataContext.class))).thenReturn(mockApiRemoteDataContext);
        when(mockApiRemoteDataContext.getEndPointId()).thenReturn("endpoint");
        doThrow(new ClouderaManagerOperationFailedException("error")).when(mgmtSetupService).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );

        ClouderaManagerOperationFailedException actual =
                assertThrows(ClouderaManagerOperationFailedException.class,
                        () -> underTest.configureManagementServices(mockTemplatePreparationObject, mockSdxContext, mockSdxStackCrn, telemetry, proxyConfig));

        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
        verify(mgmtSetupService, times(1)).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );
    }

    @Test
    void testConfigureManagementServicesWhenThePrimaryGatewayInstanceDiscoveryFQDNIsPresentedOnCMSideShoudCallSetupMgmtServices() throws Exception {
        HostsResourceApi mockHostsResourceApi = mock(HostsResourceApi.class);
        TemplatePreparationObject mockTemplatePreparationObject = mock(TemplatePreparationObject.class);
        String mockSdxContext = JsonUtil.writeValueAsString(new ApiRemoteDataContext());
        String mockSdxStackCrn = "mockSdxStackCrn";
        Telemetry telemetry = mock(Telemetry.class);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        ApiClient apiClient = mock(ApiClient.class);
        ApiHostList apiHostList = mock(ApiHostList.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        CdpResourceApi cdpResourceApi = mock(CdpResourceApi.class);
        ApiRemoteDataContext mockApiRemoteDataContext = mock(ApiRemoteDataContext.class);


        when(mockHostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()))
                .thenReturn(apiHostList);
        when(mockTemplatePreparationObject.getGeneralClusterConfigs())
                .thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN())
                .thenReturn(Optional.of("fqdn"));
        when(apiHostList.getItems()).thenReturn(List.of(apiHost("fqdn")));
        when(clouderaManagerApiClientProvider.getRootClient(any(Integer.class), anyString(), anyString(), any(HttpClientConfig.class)))
                .thenReturn(apiClient);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(mockHostsResourceApi);
        when(clouderaManagerApiFactory.getCdpResourceApi(any(ApiClient.class))).thenReturn(cdpResourceApi);
        when(cdpResourceApi.postRemoteContext(any(ApiRemoteDataContext.class))).thenReturn(mockApiRemoteDataContext);
        when(mockApiRemoteDataContext.getEndPointId()).thenReturn("endpoint");
        doNothing().when(mgmtSetupService).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );

        underTest.configureManagementServices(mockTemplatePreparationObject, mockSdxContext, mockSdxStackCrn, telemetry, proxyConfig);

        verify(mgmtSetupService, times(1)).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );
    }

    @Test
    void testConfigureManagementServicesWhenThePrimaryGatewayInstanceDiscoveryFQDNIsNOTPresentedOnCMSideShoudNOTCallSetupMgmtServices() throws Exception {
        HostsResourceApi mockHostsResourceApi = mock(HostsResourceApi.class);
        TemplatePreparationObject mockTemplatePreparationObject = mock(TemplatePreparationObject.class);
        String mockSdxContext = JsonUtil.writeValueAsString(new ApiRemoteDataContext());
        String mockSdxStackCrn = "mockSdxStackCrn";
        Telemetry telemetry = mock(Telemetry.class);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        ApiClient apiClient = mock(ApiClient.class);
        ApiHostList apiHostList = mock(ApiHostList.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        CdpResourceApi cdpResourceApi = mock(CdpResourceApi.class);
        ApiRemoteDataContext mockApiRemoteDataContext = mock(ApiRemoteDataContext.class);


        when(mockHostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()))
                .thenReturn(apiHostList);
        when(mockTemplatePreparationObject.getGeneralClusterConfigs())
                .thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN())
                .thenReturn(Optional.of("fqdn1"));
        when(apiHostList.getItems()).thenReturn(List.of(apiHost("fqdn")));
        when(clouderaManagerApiClientProvider.getRootClient(any(Integer.class), anyString(), anyString(), any(HttpClientConfig.class)))
                .thenReturn(apiClient);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(mockHostsResourceApi);
        when(clouderaManagerApiFactory.getCdpResourceApi(any(ApiClient.class))).thenReturn(cdpResourceApi);
        when(cdpResourceApi.postRemoteContext(any(ApiRemoteDataContext.class))).thenReturn(mockApiRemoteDataContext);
        when(mockApiRemoteDataContext.getEndPointId()).thenReturn("endpoint");

        underTest.configureManagementServices(mockTemplatePreparationObject, mockSdxContext, mockSdxStackCrn, telemetry, proxyConfig);

        verify(mgmtSetupService, times(0)).setupMgmtServices(
                any(Stack.class),
                any(ApiClient.class),
                any(ApiHostRef.class),
                any(Telemetry.class),
                anyString(),
                anyString(),
                any(ProxyConfig.class)
        );
    }

    @Test
    void testConfigureKerberosWhenCMVersionIsLowerThen630ShouldCallConfigureKerberos() throws Exception {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.2.0");

        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);
        doNothing().when(kerberosService)
                .configureKerberosViaApi(
                        any(ApiClient.class),
                        any(HttpClientConfig.class),
                        any(Stack.class),
                        any(KerberosConfig.class)
                );

        underTest.configureKerberos(kerberosConfig);

        verify(kerberosService, times(1)).configureKerberosViaApi(
                any(ApiClient.class),
                any(HttpClientConfig.class),
                any(Stack.class),
                any(KerberosConfig.class)
        );
    }

    @Test
    void testConfigureKerberosWhenCMVersionIsHigherThen630ShouldCallConfigureKerberos() throws Exception {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.5.0");

        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);

        underTest.configureKerberos(kerberosConfig);

        verify(kerberosService, times(0)).configureKerberosViaApi(
                any(ApiClient.class),
                any(HttpClientConfig.class),
                any(Stack.class),
                any(KerberosConfig.class)
        );
    }

    @Test
    void testConfigureKerberosWhenThrowApiExceptionThenShouldThrowClouderaManagerOperationFailedException() throws Exception {
        ApiException error = mock(ApiException.class);
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.2.0");

        when(error.getResponseBody()).thenReturn(null);
        when(error.getMessage()).thenReturn("error");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);
        doThrow(error).when(kerberosService)
                .configureKerberosViaApi(
                        any(ApiClient.class),
                        any(HttpClientConfig.class),
                        any(Stack.class),
                        any(KerberosConfig.class)
                );

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.configureKerberos(kerberosConfig));

        verify(kerberosService, times(1)).configureKerberosViaApi(
                any(ApiClient.class),
                any(HttpClientConfig.class),
                any(Stack.class),
                any(KerberosConfig.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testConfigureKerberosWhenThrowClouderaManagerOperationFailedExceptionThenShouldThrowClouderaManagerOperationFailedException() throws Exception {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.2.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);
        doThrow(new ClouderaManagerOperationFailedException("error")).when(kerberosService)
                .configureKerberosViaApi(
                        any(ApiClient.class),
                        any(HttpClientConfig.class),
                        any(Stack.class),
                        any(KerberosConfig.class)
                );

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.configureKerberos(kerberosConfig));

        verify(kerberosService, times(1)).configureKerberosViaApi(
                any(ApiClient.class),
                any(HttpClientConfig.class),
                any(Stack.class),
                any(KerberosConfig.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testUpdateConfigWhenUpdateConfigShouldCall() throws Exception {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.updateConfig(anyString(), any(ApiConfigList.class)))
                .thenReturn(new ApiConfigList());

        underTest.updateConfig();

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                anyString(),
                any(ApiConfigList.class)
        );
    }

    @Test
    void testUpdateConfigWhenUpdateConfigWithDmpEntitlement() throws Exception {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        lenient().when(entitlementService.isObservabilityDmpEnabled(any())).thenReturn(true);
        lenient().when(entitlementService.isObservabilityRealTimeJobsEnabled(any())).thenReturn(true);
        lenient().when(entitlementService.isObservabilitySaasPremiumEnabled(any())).thenReturn(false);
        lenient().when(entitlementService.isObservabilitySaasTrialEnabled(any())).thenReturn(false);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.updateConfig(anyString(), any(ApiConfigList.class)))
                .thenReturn(new ApiConfigList());

        underTest.updateConfig();

        verify(clouderaManagerResourceApi, times(2)).updateConfig(
                anyString(),
                any(ApiConfigList.class)
        );
    }

    @Test
    void testUpdateConfigWhenUpdateConfigWithAllObservabilityEntitlement() throws Exception {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        lenient().when(entitlementService.isObservabilityDmpEnabled(any())).thenReturn(true);
        lenient().when(entitlementService.isObservabilityRealTimeJobsEnabled(any())).thenReturn(true);
        lenient().when(entitlementService.isObservabilitySaasPremiumEnabled(any())).thenReturn(true);
        lenient().when(entitlementService.isObservabilitySaasTrialEnabled(any())).thenReturn(true);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.updateConfig(anyString(), any(ApiConfigList.class)))
                .thenReturn(new ApiConfigList());

        underTest.updateConfig();

        verify(clouderaManagerResourceApi, times(3)).updateConfig(
                anyString(),
                any(ApiConfigList.class)
        );
    }

    @Test
    void testUpdateConfigWhenThrowApiExceptionThenThrowClouderaManagerOperationFailedException() throws Exception {
        ApiException error = mock(ApiException.class);
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(error.getResponseBody()).thenReturn(null);
        when(error.getMessage()).thenReturn("error");

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        doThrow(error).when(clouderaManagerResourceApi).updateConfig(anyString(), any(ApiConfigList.class));

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.updateConfig());

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                anyString(),
                any(ApiConfigList.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testUpdateConfigWhenThrowClouderaManagerOperationFailedExceptionThenThrowClouderaManagerOperationFailedException() throws Exception {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        doThrow(new ClouderaManagerOperationFailedException("error"))
                .when(clouderaManagerResourceApi).updateConfig(anyString(), any(ApiConfigList.class));


        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.updateConfig());

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                anyString(),
                any(ApiConfigList.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testRefreshParcelReposWithPreWarmedImageShouldCallStartPollingCmParcelRepositoryRefresh() throws Exception {
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        ApiCommand apiCommand = mock(ApiCommand.class);

        when(clouderaManagerRepo.getPredefined()).thenReturn(true);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class))).thenReturn(clouderaManagerResourceApi);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(
                any(Stack.class),
                any(ApiClient.class),
                any(BigDecimal.class)
        )).thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.refreshParcelRepos();

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmParcelRepositoryRefresh(
                any(Stack.class),
                any(ApiClient.class),
                any(BigDecimal.class)
        );
    }

    @Test
    void testRefreshParcelReposWithNONPreWarmedImageShouldNotCallStartPollingCmParcelRepositoryRefresh() {
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);

        when(clouderaManagerRepo.getPredefined()).thenReturn(false);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);

        underTest.refreshParcelRepos();

        verify(clouderaManagerPollingServiceProvider, times(0)).startPollingCmParcelRepositoryRefresh(
                any(Stack.class),
                any(ApiClient.class),
                any(BigDecimal.class)
        );
    }

    @Test
    void testRefreshParcelReposWhenApiExceptionOccursShouldThrowCloudbreakServiceException() {
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);

        when(clouderaManagerRepo.getPredefined()).thenReturn(true);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        doThrow(new ClouderaManagerOperationFailedException("error")).when(clouderaManagerApiFactory).getClouderaManagerResourceApi(any(ApiClient.class));

        CloudbreakServiceException actual =
                assertThrows(CloudbreakServiceException.class, () -> underTest.refreshParcelRepos());

        assertEquals(CloudbreakServiceException.class, actual.getClass());
    }

    @Test
    void testWaitForHostsWhenEverythingFineShouldCmHostStatus() throws Exception {
        ApiClient apiClient = mock(ApiClient.class);

        when(clouderaManagerApiClientProvider.getV31Client(anyInt(), anyString(), anyString(), any(HttpClientConfig.class)))
                .thenReturn(apiClient);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(any(Stack.class), any(ApiClient.class), anyList()))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build());

        underTest.waitForHosts(Set.of());

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(
                any(Stack.class), any(ApiClient.class), anyList());
    }

    @Test
    void testWaitForHostsWhenDropClouderaManagerClientInitExceptionShouldReturnClusterClientInitException() throws Exception {
        doThrow(new ClouderaManagerClientInitException()).when(clouderaManagerApiClientProvider)
                .getV31Client(anyInt(), anyString(), anyString(), any(HttpClientConfig.class));

        ClusterClientInitException actual =
                assertThrows(ClusterClientInitException.class, () -> underTest.waitForHosts(Set.of()));

        assertEquals(ClusterClientInitException.class, actual.getClass());
    }

    @Test
    void testSupressWarningShouldWorkFine() {
        doNothing().when(clouderaManagerYarnSetupService).suppressWarnings(any(Stack.class), any(ApiClient.class));

        underTest.suppressWarnings();

        verify(clouderaManagerYarnSetupService, times(1)).suppressWarnings(
                any(Stack.class),
                any(ApiClient.class)
        );
    }

    @Test
    void testWaitForServerWhenPollingExitedThenShouldReturnWithCancellationException() {
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(any(Stack.class), any(ApiClient.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build());

        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.waitForServer(false));

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(
                any(Stack.class),
                any(ApiClient.class)
        );
        assertEquals(CancellationException.class, actual.getClass());
    }

    @Test
    void testWaitForServerWhenPollingFailedThenShouldReturnWithCloudbreakException() {
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(any(Stack.class), any(ApiClient.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().failure().build());

        CloudbreakException actual = assertThrows(CloudbreakException.class, () -> underTest.waitForServer(false));

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(
                any(Stack.class),
                any(ApiClient.class)
        );
        assertEquals(CloudbreakException.class, actual.getClass());
    }

    @Test
    void testWaitForServerWhenPollingSuccessThenEverythingShouldWork() throws ClusterClientInitException, CloudbreakException {
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(any(Stack.class), any(ApiClient.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.waitForServer(false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(
                any(Stack.class),
                any(ApiClient.class)
        );
    }

    @Test
    void testStartManagementServicesWhenStartSuccessThenShouldEverythingWorksfine() throws ApiException {
        doNothing().when(clouderaManagerMgmtLaunchService).startManagementServices(any(Stack.class), any(ApiClient.class));

        underTest.startManagementServices();

        verify(clouderaManagerMgmtLaunchService, times(1)).startManagementServices(
                any(Stack.class),
                any(ApiClient.class)
        );
    }

    @Test
    void testStartManagementServicesWhenApiExceptionHappensThenShouldReturnClouderaManagerOperationFailedException() throws ApiException {
        ApiException error = mock(ApiException.class);

        when(error.getResponseBody()).thenReturn(null);
        when(error.getMessage()).thenReturn("error");
        doThrow(error).when(clouderaManagerMgmtLaunchService)
                .startManagementServices(any(Stack.class), any(ApiClient.class));

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.startManagementServices());

        verify(clouderaManagerMgmtLaunchService, times(1)).startManagementServices(
                any(Stack.class),
                any(ApiClient.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testConfigureSupportTagsWhenCmHostPresentedShouldEverythingWorks() throws ApiException {
        HostsResourceApi mockHostsResourceApi = mock(HostsResourceApi.class);
        ApiHostList apiHostList = mock(ApiHostList.class);
        TemplatePreparationObject mockTemplatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(mockTemplatePreparationObject.getGeneralClusterConfigs())
                .thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN())
                .thenReturn(Optional.of("fqdn"));
        when(apiHostList.getItems()).thenReturn(List.of(apiHost("fqdn")));
        when(mockHostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()))
                .thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(mockHostsResourceApi);
        doNothing().when(clouderaManagerSupportSetupService).prepareSupportRole(any(ApiClient.class), any(StackType.class));

        underTest.configureSupportTags(mockTemplatePreparationObject);

        verify(clouderaManagerSupportSetupService, times(1)).prepareSupportRole(
                any(ApiClient.class),
                any(StackType.class)
        );
    }

    @Test
    void testConfigureSupportTagsWhenThrowCManagerOperationFailedExceptionShouldThrowCManagerOperationFailedException() throws ApiException {
        HostsResourceApi mockHostsResourceApi = mock(HostsResourceApi.class);
        ApiHostList apiHostList = mock(ApiHostList.class);
        TemplatePreparationObject mockTemplatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);

        when(mockTemplatePreparationObject.getGeneralClusterConfigs())
                .thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN())
                .thenReturn(Optional.of("fqdn"));
        when(apiHostList.getItems()).thenReturn(List.of(apiHost("fqdn")));
        when(mockHostsResourceApi.readHosts(null, null, DataView.SUMMARY.name()))
                .thenReturn(apiHostList);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(mockHostsResourceApi);
        doThrow(new ClouderaManagerOperationFailedException("error")).when(clouderaManagerSupportSetupService)
                .prepareSupportRole(any(ApiClient.class), any(StackType.class));

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.configureSupportTags(mockTemplatePreparationObject));

        verify(clouderaManagerSupportSetupService, times(1)).prepareSupportRole(
                any(ApiClient.class),
                any(StackType.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testSetupProxyWhenProxyPresentedShouldEverythingWorksFineButNoProxyHostBecauseOfVersion() throws ApiException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.updateConfig(anyString(), any(ApiConfigList.class)))
                .thenReturn(new ApiConfigList());

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.1.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);

        underTest.setupProxy(testProxyConfig());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ApiConfigList> configsCaptor = ArgumentCaptor.forClass(ApiConfigList.class);

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                messageCaptor.capture(),
                configsCaptor.capture()
        );

        String capturedMessage = messageCaptor.getValue();
        ApiConfigList capturedConfigs = configsCaptor.getValue();

        assertThat(capturedMessage).isEqualTo("Update proxy settings");
        assertThat(capturedConfigs.getItems()).containsExactlyInAnyOrder(
                new ApiConfig().name("parcel_proxy_server").value(PROXY_HOST),
                new ApiConfig().name("parcel_proxy_port").value(Integer.toString(PROXY_PORT)),
                new ApiConfig().name("parcel_proxy_protocol").value(PROXY_PROTOCOL.toUpperCase(Locale.ROOT)),
                new ApiConfig().name("parcel_proxy_user").value(PROXY_USER),
                new ApiConfig().name("parcel_proxy_password").value(PROXY_PASSWORD)
        );
    }

    @Test
    void testSetupProxyWhenProxyPresentedShouldEverythingWorksFineWithNoProxyHost() throws ApiException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.updateConfig(anyString(), any(ApiConfigList.class)))
                .thenReturn(new ApiConfigList());

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.6.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);

        underTest.setupProxy(testProxyConfig());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ApiConfigList> configsCaptor = ArgumentCaptor.forClass(ApiConfigList.class);

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                messageCaptor.capture(),
                configsCaptor.capture()
        );

        String capturedMessage = messageCaptor.getValue();
        ApiConfigList capturedConfigs = configsCaptor.getValue();

        assertThat(capturedMessage).isEqualTo("Update proxy settings");
        assertThat(capturedConfigs.getItems()).containsExactlyInAnyOrder(
                new ApiConfig().name("parcel_proxy_server").value(PROXY_HOST),
                new ApiConfig().name("parcel_proxy_port").value(Integer.toString(PROXY_PORT)),
                new ApiConfig().name("parcel_proxy_protocol").value(PROXY_PROTOCOL.toUpperCase(Locale.ROOT)),
                new ApiConfig().name("parcel_proxy_user").value(PROXY_USER),
                new ApiConfig().name("parcel_proxy_password").value(PROXY_PASSWORD),
                new ApiConfig().name("parcel_no_proxy_list").value(PROXY_NO_PROXY_HOSTS)
        );
    }

    @Test
    void testSetupProxyWhenProxyConfigIsNull() throws ApiException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.updateConfig(anyString(), any(ApiConfigList.class)))
                .thenReturn(new ApiConfigList());

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.6.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);

        underTest.setupProxy(null);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ApiConfigList> configsCaptor = ArgumentCaptor.forClass(ApiConfigList.class);

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                messageCaptor.capture(),
                configsCaptor.capture()
        );

        String capturedMessage = messageCaptor.getValue();
        ApiConfigList capturedConfigs = configsCaptor.getValue();

        assertThat(capturedMessage).isEqualTo("Update proxy settings");
        assertThat(capturedConfigs.getItems()).containsExactlyInAnyOrder(
                new ApiConfig().name("parcel_proxy_server").value(""),
                new ApiConfig().name("parcel_proxy_port").value(""),
                new ApiConfig().name("parcel_proxy_protocol").value(""),
                new ApiConfig().name("parcel_proxy_user").value(""),
                new ApiConfig().name("parcel_proxy_password").value(""),
                new ApiConfig().name("parcel_no_proxy_list").value("")
        );
    }

    @Test
    void testSetupProxyWhenProxysetupThrowApiExceptionShouldThrowClouderaManagerOperationFailedException() throws ApiException {
        ApiException error = mock(ApiException.class);
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        doThrow(error).when(clouderaManagerResourceApi).updateConfig(anyString(), any(ApiConfigList.class));

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.1.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.setupProxy(testProxyConfig()));

        verify(clouderaManagerResourceApi, times(1)).updateConfig(
                anyString(),
                any(ApiConfigList.class)
        );
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testInstallClusterWhenApiExceptionOccursShouldReturnClouderaManagerOperationFailedException() throws ApiException {
        ClustersResourceApi clustersResourceApi = mock(ClustersResourceApi.class);
        ApiException error = mock(ApiException.class);

        when(error.getResponseBody()).thenReturn(null);
        when(error.getMessage()).thenReturn("error");
        when(clouderaManagerApiFactory.getClustersResourceApi(any(ApiClient.class))).thenReturn(clustersResourceApi);
        doThrow(error).when(clustersResourceApi).readCluster(anyString());

        ClouderaManagerOperationFailedException actual = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.installCluster(""));
        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
    }

    @Test
    void testInstallClusterWhenEverythingWorksFineShouldPollTheInsallProgress() throws ApiException {
        ClustersResourceApi clustersResourceApi = mock(ClustersResourceApi.class);
        ApiCommand apiCommand = mock(ApiCommand.class);
        ApiCluster apiCluster = mock(ApiCluster.class);
        ClusterCommand clusterCommand = mock(ClusterCommand.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.2.0");
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);
        when(clouderaManagerApiFactory.getClustersResourceApi(any(ApiClient.class))).thenReturn(clustersResourceApi);
        when(clustersResourceApi.readCluster(anyString())).thenReturn(apiCluster);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), any(ClusterCommandType.class)))
                .thenReturn(Optional.empty());
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(clusterCommand.getCommandId()).thenReturn(BigDecimal.ONE);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.importClusterTemplate(anyBoolean(), any(ApiClusterTemplate.class))).thenReturn(apiCommand);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenReturn(clusterCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmTemplateInstallation(any(Stack.class), any(ApiClient.class), any(BigDecimal.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build());

        underTest.installCluster("{}");

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmTemplateInstallation(
                any(Stack.class), any(ApiClient.class), any(BigDecimal.class));
        verify(clusterCommandService, times(1)).save(any(ClusterCommand.class));
    }

    @Test
    void testRetryInstallClusterWhenOriginalSucceeded() throws ApiException {
        ClustersResourceApi clustersResourceApi = mock(ClustersResourceApi.class);
        ApiCommand apiCommand = mock(ApiCommand.class);
        ApiCluster apiCluster = mock(ApiCluster.class);
        ClusterCommand clusterCommand = mock(ClusterCommand.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.2.0");
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);
        when(clouderaManagerApiFactory.getClustersResourceApi(any(ApiClient.class))).thenReturn(clustersResourceApi);
        when(clustersResourceApi.readCluster(anyString())).thenReturn(apiCluster);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), any(ClusterCommandType.class)))
                .thenReturn(Optional.of(clusterCommand));
        when(apiCommand.getSuccess()).thenReturn(Boolean.TRUE);
        when(clusterCommand.getCommandId()).thenReturn(BigDecimal.ONE);
        when(clouderaManagerCommandsService.getApiCommand(any(), any())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmTemplateInstallation(any(Stack.class), any(ApiClient.class), any(BigDecimal.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build());

        underTest.installCluster("{}");

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmTemplateInstallation(
                any(Stack.class), any(ApiClient.class), any(BigDecimal.class));
        verify(clouderaManagerCommandsService).getApiCommand(any(), eq(BigDecimal.ONE));
        verify(clouderaManagerCommandsService, times(0)).retryApiCommand(any(), any());
    }

    @Test
    void testRetryInstallClusterWhenOriginalFailed() throws ApiException {
        ClustersResourceApi clustersResourceApi = mock(ClustersResourceApi.class);
        ApiCommand apiCommand = mock(ApiCommand.class);
        ApiCluster apiCluster = mock(ApiCluster.class);
        ClusterCommand clusterCommand = mock(ClusterCommand.class);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.2.0");
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong()))
                .thenReturn(clouderaManagerRepo);
        when(clouderaManagerApiFactory.getClustersResourceApi(any(ApiClient.class))).thenReturn(clustersResourceApi);
        when(clustersResourceApi.readCluster(anyString())).thenReturn(apiCluster);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), any(ClusterCommandType.class)))
                .thenReturn(Optional.of(clusterCommand));
        when(apiCommand.getSuccess()).thenReturn(Boolean.FALSE);
        when(apiCommand.getActive()).thenReturn(Boolean.FALSE);
        when(apiCommand.getCanRetry()).thenReturn(Boolean.TRUE);
        when(clusterCommand.getCommandId()).thenReturn(BigDecimal.ONE);
        when(clouderaManagerCommandsService.getApiCommand(any(), any())).thenReturn(apiCommand);
        when(clouderaManagerCommandsService.retryApiCommand(any(), any())).thenReturn(apiCommand);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenReturn(clusterCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmTemplateInstallation(any(Stack.class), any(ApiClient.class), any(BigDecimal.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build());

        underTest.installCluster("{}");

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmTemplateInstallation(
                any(Stack.class), any(ApiClient.class), any(BigDecimal.class));
        verify(clouderaManagerCommandsService).getApiCommand(any(), eq(BigDecimal.ONE));
        verify(clouderaManagerCommandsService).retryApiCommand(any(), eq(BigDecimal.ONE));
    }

    @Test
    void getCMHosFromMGMTWithEnterpiseDL() throws ApiException, IOException {
        ClouderaManagerSetupService spy = spy(underTest);

        ApiHostList apiHostList = getApiHostList();

        String template = FileReaderUtils.readFileFromPath(Path.of("../core/src/main/resources/defaults/blueprints/7.2.17/cdp-sdx-enterprise.bp"));

        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);

        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);
        when(blueprintView.getVersion()).thenReturn("7.2.17");
        when(blueprintUtils.isEnterpriseDatalake(any(TemplatePreparationObject.class))).thenReturn(true);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq((String) null), eq((String) null), eq(DataView.SUMMARY.name()))).thenReturn(apiHostList);
        doNothing().when(mgmtSetupService).setupMgmtServices(any(), any(), any(), any(), any(), any(), any());
        spy.configureManagementServices(templatePreparationObject, null, null, null, null);
        verify(spy, times(1)).getAuxiliaryHost(any(), any());
    }

    @Test
    void getCMHosFromMGMTWithEnterpiseDLHostNotFound() throws ApiException, IOException {
        ClouderaManagerSetupService spy = spy(underTest);

        ApiHostList apiHostList = getApiHostList();
        apiHostList.getItems().clear();

        String template = FileReaderUtils.readFileFromPath(Path.of("../core/src/main/resources/defaults/blueprints/7.2.17/cdp-sdx-enterprise.bp"));

        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);

        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);
        when(blueprintView.getVersion()).thenReturn("7.2.17");
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq((String) null), eq((String) null), eq(DataView.SUMMARY.name()))).thenReturn(apiHostList);
        when(blueprintUtils.isEnterpriseDatalake(any(TemplatePreparationObject.class))).thenReturn(true);

        spy.configureManagementServices(templatePreparationObject, null, null, null, null);

        verify(spy, times(1)).getAuxiliaryHost(any(), any());
        verify(mgmtSetupService, times(0)).setupMgmtServices(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCMHosFromMGMTWithEnterpiseDH() throws ApiException {
        ClouderaManagerSetupService spy = spy(underTest);

        ApiHostList apiHostList = getApiHostList();

        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);

        when(templatePreparationObject.getStackType()).thenReturn(StackType.WORKLOAD);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN()).thenReturn(Optional.of("fqdn"));
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq((String) null), eq((String) null), eq(DataView.SUMMARY.name()))).thenReturn(apiHostList);
        doNothing().when(mgmtSetupService).setupMgmtServices(any(), any(), any(), any(), any(), any(), any());

        spy.configureManagementServices(templatePreparationObject, null, null, null, null);

        verify(spy, times(0)).getAuxiliaryHost(any(), any());
    }

    @Test
    void getCMHosFromMGMTWithEnterpiseOldVersonDL() throws ApiException, IOException {
        ClouderaManagerSetupService spy = spy(underTest);

        ApiHostList apiHostList = getApiHostList();

        String template = FileReaderUtils.readFileFromPath(Path.of("../datalake/src/main/resources/duties/7.2.16/aws/medium_duty_ha.json"));

        TemplatePreparationObject templatePreparationObject = mock(TemplatePreparationObject.class);
        GeneralClusterConfigs generalClusterConfigs = mock(GeneralClusterConfigs.class);
        BlueprintView blueprintView = mock(BlueprintView.class);
        HostsResourceApi hostsResourceApi = mock(HostsResourceApi.class);

        when(templatePreparationObject.getStackType()).thenReturn(StackType.DATALAKE);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN()).thenReturn(Optional.of("fqdn"));
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);
        when(blueprintView.getVersion()).thenReturn("7.2.17");
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq((String) null), eq((String) null), eq(DataView.SUMMARY.name()))).thenReturn(apiHostList);
        doNothing().when(mgmtSetupService).setupMgmtServices(any(), any(), any(), any(), any(), any(), any());
        when(blueprintUtils.isEnterpriseDatalake(any(TemplatePreparationObject.class))).thenReturn(false);
        spy.configureManagementServices(templatePreparationObject, null, null, null, null);

        verify(spy, times(0)).getAuxiliaryHost(any(), any());
    }

    private ApiHostList getApiHostList() {
        ApiHostList apiHostList = new ApiHostList();
        apiHostList.addItemsItem(apiHost("auxiliary"));
        apiHostList.addItemsItem(apiHost("fqdn"));
        return apiHostList;
    }

    private ApiHost apiHost(String fqdn) {
        ApiHost apiHost = new ApiHost();
        apiHost.setHostname(fqdn);
        return apiHost;
    }

    private ProxyConfig testProxyConfig() {
        return ProxyConfig.builder().withCrn("crn")
                .withName("proxy")
                .withProtocol(PROXY_PROTOCOL)
                .withServerHost(PROXY_HOST)
                .withServerPort(PROXY_PORT)
                .withProxyAuthentication(ProxyAuthentication.builder()
                        .withUserName(PROXY_USER)
                        .withPassword(PROXY_PASSWORD)
                        .build())
                .withNoProxyHosts(PROXY_NO_PROXY_HOSTS)
                .build();
    }

    private Cluster testCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("name");
        cluster.setCloudbreakClusterManagerPassword("pass");
        cluster.setCloudbreakClusterManagerUser("user");
        return cluster;
    }

    private Stack testStack() {
        Stack stack = new Stack();
        User user = new User();
        user.setUserId("1");
        user.setId(1L);
        user.setUserName("testJoska");
        user.setUserCrn("joska::crn");
        user.setTenant(new Tenant());
        stack.setCreator(user);
        stack.setGatewayPort(1);
        stack.setCluster(testCluster());
        stack.setType(StackType.DATALAKE);
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");
        return stack;
    }
}
