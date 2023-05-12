package com.sequenceiq.cloudbreak.cm;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.error.mapper.ClouderaManagerStorageErrorMapper;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
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
import com.sequenceiq.cloudbreak.repository.ClusterCommandRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerSetupServiceTest {

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
    private ClusterCommandRepository clusterCommandRepository;

    @Mock
    private ClouderaManagerStorageErrorMapper clouderaManagerStorageErrorMapper;

    @Mock
    private ClouderaManagerFedRAMPService clouderaManagerFedRAMPService;

    @InjectMocks
    private ClouderaManagerSetupService underTest;

    @BeforeEach
    public void before() {
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
        ReflectionTestUtils.setField(underTest, "clusterCommandRepository", clusterCommandRepository);
        ReflectionTestUtils.setField(underTest, "clouderaManagerFedRAMPService", clouderaManagerFedRAMPService);
        ReflectionTestUtils.setField(underTest, "apiClient", mock(ApiClient.class));
    }

    @Test
    public void testAutoconfigureWhenItDoesItsJob() throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = mock(MgmtServiceResourceApi.class);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any(ApiClient.class)))
                .thenReturn(mgmtServiceResourceApi);

        underTest.autoConfigureClusterManager();

        verify(mgmtServiceResourceApi, times(1)).autoConfigure();
    }

    @Test
    public void testAutoconfigureWhenThrowsException() throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = mock(MgmtServiceResourceApi.class);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any(ApiClient.class)))
                .thenReturn(mgmtServiceResourceApi);
        doThrow(ApiException.class).when(mgmtServiceResourceApi).autoConfigure();

        Assertions.assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.autoConfigureClusterManager());
        verify(mgmtServiceResourceApi, times(1)).autoConfigure();
    }

    @Test
    public void testValidateLicenceWhenEverythingWorkAsExpectedShouldNotThrowException() {
        doNothing().when(clouderaManagerLicenseService).validateClouderaManagerLicense(any());

        underTest.validateLicence();

        verify(clouderaManagerLicenseService, times(1)).validateClouderaManagerLicense(any());
    }

    @Test
    public void testPublishPolicyWhenGovCloudAndHigherThan792() throws ApiException, IOException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        ArgumentCaptor<ApiConfigPolicy> argumentCaptor = ArgumentCaptor.forClass(ApiConfigPolicy.class);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerFedRAMPService.getApiConfigPolicy()).thenReturn(new ApiConfigPolicy());

        when(clouderaManagerResourceApi.addConfigPolicy(argumentCaptor.capture())).thenReturn(new ApiConfigPolicy());

        underTest.publishPolicy("{\"cdhVersion\":\"7.2.16\",\"cmVersion\":\"7.9.2\",\"displayName\":\"opdb\"}", true);

        verify(clouderaManagerResourceApi, times(1)).addConfigPolicy(any());

    }

    @Test
    public void testPublishPolicyWhenNonGovCloudAndHigherThan792() throws ApiException, IOException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);

        underTest.publishPolicy("{\"cdhVersion\":\"7.2.16\",\"cmVersion\":\"7.9.2\",\"displayName\":\"opdb\"}", false);

        verify(clouderaManagerResourceApi, times(0)).addConfigPolicy(any());
    }

    @Test
    public void testPublishPolicyWhenGovCloudAndLowerThan792() throws ApiException, IOException {
        ClouderaManagerResourceApi clouderaManagerResourceApi = mock(ClouderaManagerResourceApi.class);
        ArgumentCaptor<ApiConfigPolicy> argumentCaptor = ArgumentCaptor.forClass(ApiConfigPolicy.class);

        underTest.publishPolicy("{\"cdhVersion\":\"7.2.16\",\"cmVersion\":\"7.9.1\",\"displayName\":\"opdb\"}", true);

        verify(clouderaManagerResourceApi, times(0)).addConfigPolicy(any());
    }

    @Test
    public void testValidateLicenceWhenItThrowExceptionShouldMapToClouderaManagerOperationFailedException() {
        doThrow(new RuntimeException()).when(clouderaManagerLicenseService).validateClouderaManagerLicense(any());

        ClouderaManagerOperationFailedException actual =
                assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.validateLicence());

        assertEquals(ClouderaManagerOperationFailedException.class, actual.getClass());
        verify(clouderaManagerLicenseService, times(1)).validateClouderaManagerLicense(any());
    }

    @Test
    public void testConfigureManagementServicesWhenApiExceptionHappensThenShouldThrowClouderaManagerOperationFailedException() throws Exception {
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
    public void testConfigureManagementServicesWhenCManagerOperationFailedExceptionHappensThenShouldThrowCManagerOperationFailedException() throws Exception {
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
    public void testConfigureManagementServicesWhenThePrimaryGatewayInstanceDiscoveryFQDNIsPresentedOnCMSideShoudCallSetupMgmtServices() throws Exception {
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
    public void testConfigureManagementServicesWhenThePrimaryGatewayInstanceDiscoveryFQDNIsNOTPresentedOnCMSideShoudNOTCallSetupMgmtServices() throws Exception {
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
    public void testConfigureKerberosWhenCMVersionIsLowerThen630ShouldCallConfigureKerberos() throws Exception {
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
    public void testConfigureKerberosWhenCMVersionIsHigherThen630ShouldCallConfigureKerberos() throws Exception {
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
    public void testConfigureKerberosWhenThrowApiExceptionThenShouldThrowClouderaManagerOperationFailedException() throws Exception {
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
    public void testConfigureKerberosWhenThrowClouderaManagerOperationFailedExceptionThenShouldThrowClouderaManagerOperationFailedException() throws Exception {
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
    public void testUpdateConfigWhenUpdateConfigShouldCall() throws Exception {
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
    public void testUpdateConfigWhenThrowApiExceptionThenThrowClouderaManagerOperationFailedException() throws Exception {
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
    public void testUpdateConfigWhenThrowClouderaManagerOperationFailedExceptionThenThrowClouderaManagerOperationFailedException() throws Exception {
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
    public void testRefreshParcelReposWithPreWarmedImageShouldCallStartPollingCmParcelRepositoryRefresh() throws Exception {
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
    public void testRefreshParcelReposWithNONPreWarmedImageShouldNotCallStartPollingCmParcelRepositoryRefresh() {
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
    public void testRefreshParcelReposWhenApiExceptionOccursShouldThrowCloudbreakServiceException() {
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);

        when(clouderaManagerRepo.getPredefined()).thenReturn(true);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        doThrow(new ClouderaManagerOperationFailedException("error")).when(clouderaManagerApiFactory).getClouderaManagerResourceApi(any(ApiClient.class));

        CloudbreakServiceException actual =
                assertThrows(CloudbreakServiceException.class, () -> underTest.refreshParcelRepos());

        assertEquals(CloudbreakServiceException.class, actual.getClass());
    }

    @Test
    public void testWaitForHostsWhenEverythingFineShouldCmHostStatus() throws Exception {
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
    public void testWaitForHostsWhenDropClouderaManagerClientInitExceptionShouldReturnClusterClientInitException() throws Exception {
        doThrow(new ClouderaManagerClientInitException()).when(clouderaManagerApiClientProvider)
                .getV31Client(anyInt(), anyString(), anyString(), any(HttpClientConfig.class));

        ClusterClientInitException actual =
                assertThrows(ClusterClientInitException.class, () -> underTest.waitForHosts(Set.of()));

        assertEquals(ClusterClientInitException.class, actual.getClass());
    }

    @Test
    public void testSupressWarningShouldWorkFine() {
        doNothing().when(clouderaManagerYarnSetupService).suppressWarnings(any(Stack.class), any(ApiClient.class));

        underTest.suppressWarnings();

        verify(clouderaManagerYarnSetupService, times(1)).suppressWarnings(
                any(Stack.class),
                any(ApiClient.class)
        );
    }

    @Test
    public void testWaitForServerWhenPollingExitedThenShouldReturnWithCancellationException() {
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
    public void testWaitForServerWhenPollingFailedThenShouldReturnWithCloudbreakException() {
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
    public void testWaitForServerWhenPollingSuccessThenEverythingShouldWork() throws ClusterClientInitException, CloudbreakException {
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(any(Stack.class), any(ApiClient.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build());

        underTest.waitForServer(false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(
                any(Stack.class),
                any(ApiClient.class)
        );
    }

    @Test
    public void testStartManagementServicesWhenStartSuccessThenShouldEverythingWorksfine() throws ApiException {
        doNothing().when(clouderaManagerMgmtLaunchService).startManagementServices(any(Stack.class), any(ApiClient.class));

        underTest.startManagementServices();

        verify(clouderaManagerMgmtLaunchService, times(1)).startManagementServices(
                any(Stack.class),
                any(ApiClient.class)
        );
    }

    @Test
    public void testStartManagementServicesWhenApiExceptionHappensThenShouldReturnClouderaManagerOperationFailedException() throws ApiException {
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
    public void testConfigureSupportTagsWhenCmHostPresentedShouldEverythingWorks() throws ApiException {
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
    public void testConfigureSupportTagsWhenThrowCManagerOperationFailedExceptionShouldThrowCManagerOperationFailedException() throws ApiException {
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
    public void testSetupProxyWhenProxyPresentedShouldEverythingWorksFineButNoProxyHostBecauseOfVersion() throws ApiException {
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
                new ApiConfig().name("parcel_proxy_protocol").value(PROXY_PROTOCOL.toUpperCase()),
                new ApiConfig().name("parcel_proxy_user").value(PROXY_USER),
                new ApiConfig().name("parcel_proxy_password").value(PROXY_PASSWORD)
        );
    }

    @Test
    public void testSetupProxyWhenProxyPresentedShouldEverythingWorksFineWithNoProxyHost() throws ApiException {
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
                new ApiConfig().name("parcel_proxy_protocol").value(PROXY_PROTOCOL.toUpperCase()),
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
    public void testSetupProxyWhenProxysetupThrowApiExceptionShouldThrowClouderaManagerOperationFailedException() throws ApiException {
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
    public void testInstallClusterWhenApiExceptionOccursShouldReturnClouderaManagerOperationFailedException() throws ApiException {
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
    public void testInstallClusterWhenEverythingWorksFineShouldPollTheInsallProgress() throws ApiException {
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
        when(clusterCommandRepository.findTopByClusterIdAndClusterCommandType(anyLong(), any(ClusterCommandType.class)))
                .thenReturn(Optional.empty());
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(clusterCommand.getCommandId()).thenReturn(BigDecimal.ONE);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any(ApiClient.class)))
                .thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.importClusterTemplate(anyBoolean(), any(ApiClusterTemplate.class))).thenReturn(apiCommand);
        when(clusterCommandRepository.save(any(ClusterCommand.class))).thenReturn(clusterCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmTemplateInstallation(any(Stack.class), any(ApiClient.class), any(BigDecimal.class)))
                .thenReturn(new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build());

        underTest.installCluster("{}");

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmTemplateInstallation(
                any(Stack.class), any(ApiClient.class), any(BigDecimal.class));
        verify(clusterCommandRepository, times(1)).save(any(ClusterCommand.class));
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
        when(blueprintView.getBlueprintText()).thenReturn(template);
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
        when(blueprintView.getBlueprintText()).thenReturn(template);
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq((String) null), eq((String) null), eq(DataView.SUMMARY.name()))).thenReturn(apiHostList);

        spy.configureManagementServices(templatePreparationObject, null, null, null, null);

        verify(spy, times(1)).getAuxiliaryHost(any(), any());
        verify(mgmtSetupService, times(0)).setupMgmtServices(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getCMHosFromMGMTWithEnterpiseDH() throws ApiException {
        ClouderaManagerSetupService spy = spy(underTest);

        var apiHostList = getApiHostList();

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
        when(clouderaManagerApiFactory.getHostsResourceApi(any(ApiClient.class))).thenReturn(hostsResourceApi);
        when(hostsResourceApi.readHosts(eq((String) null), eq((String) null), eq(DataView.SUMMARY.name()))).thenReturn(apiHostList);
        doNothing().when(mgmtSetupService).setupMgmtServices(any(), any(), any(), any(), any(), any(), any());

        spy.configureManagementServices(templatePreparationObject, null, null, null, null);

        verify(spy, times(0)).getAuxiliaryHost(any(), any());
    }

    private ApiHostList getApiHostList() {
        var apiHostList = new ApiHostList();
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
        cluster.setCloudbreakAmbariPassword("pass");
        cluster.setCloudbreakAmbariUser("user");
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
