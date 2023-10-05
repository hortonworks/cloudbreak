package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.MgmtRoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.monitoring.ExporterConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;

public class ClouderaManagerMgmtTelemetryServiceTest {

    private static final Integer EXPORTER_PORT = 61010;

    private static final String SDX_STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    @InjectMocks
    private ClouderaManagerMgmtTelemetryService underTest;

    @Mock
    private ClouderaManagerExternalAccountService externalAccountService;

    @Mock
    private ClouderaManagerDatabusService clouderaManagerDatabusService;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerResourceApi cmResourceApi;

    @Mock
    private MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private ExporterConfiguration cmMonitoringConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSetupTelemetry() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        User user = new User();
        user.setUserCrn("crn:cdp:iam:us-west-1:accountId:user:name");
        stack.setCreator(user);
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");
        WorkloadAnalytics wa = new WorkloadAnalytics();
        Telemetry telemetry = new Telemetry();
        telemetry.setWorkloadAnalytics(wa);
        ApiConfigList apiConfigList = new ApiConfigList();
        ApiResponse response = new ApiResponse<>(0, null, apiConfigList);
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(entitlementService.useDataBusCNameEndpointEnabled(anyString())).thenReturn(false);
        when(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).thenReturn("https://dbusapi.us-west-1.sigma.altus.cloudera.com");
        when(apiClient.execute(any(), any())).thenReturn(response);
        when(clouderaManagerDatabusService.getAltusCredential(stack, SDX_STACK_CRN)).thenReturn(credential);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(cmResourceApi);
        when(cmResourceApi.updateConfig(anyString(), any())).thenReturn(apiConfigList);
        // WHEN
        underTest.setupTelemetryRole(stack, apiClient, null, new ApiRoleList(), telemetry, SDX_STACK_CRN);
        // THEN
        verify(externalAccountService, times(1)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
        verify(clouderaManagerDatabusService, times(1)).getAltusCredential(stack, SDX_STACK_CRN);
    }

    @Test
    public void testSetupTelemetryWaDisabled() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        Telemetry telemetry = new Telemetry();
        // WHEN
        underTest.setupTelemetryRole(stack, null, null, null, telemetry, SDX_STACK_CRN);
        // THEN
        verify(externalAccountService, times(0)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
    }

    @Test
    public void testSetupTelemetryForDatalake() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        WorkloadAnalytics wa = new WorkloadAnalytics();
        Telemetry telemetry = new Telemetry();
        telemetry.setWorkloadAnalytics(wa);
        when(entitlementService.useDataBusCNameEndpointEnabled(anyString())).thenReturn(false);
        when(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).thenReturn("https://dbusapi.us-west-1.sigma.altus.cloudera.com");
        // WHEN
        underTest.setupTelemetryRole(stack, null, null, null, telemetry, SDX_STACK_CRN);
        // THEN
        verify(externalAccountService, times(0)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
    }

    @Test
    public void testBuildTelemetryCMConfigList() {
        // GIVEN
        // WHEN
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        ApiConfigList result = underTest.buildTelemetryCMConfigList(workloadAnalytics, null);
        // THEN
        assertEquals(4, result.getItems().size());
        assertTrue(containsConfigWithValue(result, "telemetry_wa", "true"));
        assertTrue(containsConfigWithValue(result, "telemetry_master", "true"));
        assertTrue(containsConfigWithValue(result, "telemetry_altus_account", "cb-altus-access"));
    }

    @Test
    public void testBuildTelemetryCMConfigListWithCustomEndpoint() {
        // GIVEN
        // WHEN
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        when(entitlementService.useDataBusCNameEndpointEnabled(anyString())).thenReturn(false);
        when(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).thenReturn("https://dbusapi.us-west-1.sigma.altus.cloudera.com");
        ApiConfigList result = underTest.buildTelemetryCMConfigList(workloadAnalytics, "customEndpoint");
        // THEN
        assertEquals(5, result.getItems().size());
        assertTrue(containsConfigWithValue(result, "telemetry_altus_url", "customEndpoint"));
    }

    @Test
    public void testBuildTelemetryConfigListWithProxyConfig() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setCluster(cluster);
        WorkloadAnalytics wa = new WorkloadAnalytics();
        ProxyConfig proxyConfig = ProxyConfig.builder()
                .withProtocol("https")
                .withServerHost("proxyServer")
                .withServerPort(80)
                .withProxyAuthentication(ProxyAuthentication.builder()
                        .withUserName("proxyUser")
                        .withPassword("proxyPassword")
                        .build())
                .withNoProxyHosts("noproxy.com")
                .build();
        // WHEN
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, null, null, proxyConfig);
        // THEN
        assertTrue(containsConfigWithValue(result, "telemetrypublisher_proxy_server", "proxyServer"));
        assertTrue(containsConfigWithValue(result, "telemetrypublisher_proxy_port", "80"));
        assertTrue(containsConfigWithValue(result, "telemetrypublisher_proxy_enabled", "true"));
        assertTrue(containsConfigWithValue(result, "telemetrypublisher_proxy_user", "proxyUser"));
        assertTrue(containsConfigWithValue(result, "telemetrypublisher_proxy_password", "proxyPassword"));
        // TODO: check no_proxy configkey
    }

    @Test
    public void testBuildTelemetryConfigListWithEnvironmentMetadataForWorkloadStack() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setType(StackType.WORKLOAD);
        stack.setCluster(cluster);
        stack.setEnvironmentCrn("envCrn");
        stack.setDatalakeCrn("datalakeCrn");
        stack.setResourceCrn("datahubCrn");
        stack.setName("datahubName");
        stack.setCloudPlatform("AWS");
        stack.setRegion("us-west-1");
        String sdxContextName = "sdxName";
        String sdxCrn = "crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId";
        WorkloadAnalytics wa = new WorkloadAnalytics();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("databus.header.environment.crn", "envCrn");
        attributes.put("databus.header.environment.name", "envName");
        attributes.put("databus.header.datalake.crn", "datalakeCrn");
        attributes.put("databus.header.datalake.name", "datalakeName");
        wa.setAttributes(attributes);
        // WHEN
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, sdxContextName, sdxCrn, null);
        // THEN
        assertTrue(containsSafatyValveWithValue(result, "databus.header.environment.crn", "envCrn"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.environment.name", "envName"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.datalake.crn", "datalakeCrn"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.datalake.name", "datalakeName"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.datahub.crn", "datahubCrn"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.datahub.name", "datahubName"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.cloudprovider.name", "AWS"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.cloudprovider.region", "us-west-1"));
    }

    @Test
    public void testBuildTelemetryConfigListWithEnvironmentMetadataForDatalakeStack() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        String sdxCrn = "crn:cdp:cloudbreak:us-west-1:someone:sdxcluster:sdxId";
        String sdxContextName = "sdxName";
        stack.setType(StackType.DATALAKE);
        stack.setCluster(cluster);
        stack.setEnvironmentCrn("envCrn");
        stack.setResourceCrn(sdxCrn);
        stack.setName(sdxContextName);
        stack.setCloudPlatform("AWS");
        stack.setRegion("us-west-1");
        WorkloadAnalytics wa = new WorkloadAnalytics();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("databus.header.environment.crn", "envCrn");
        attributes.put("databus.header.environment.name", "envName");
        attributes.put("databus.header.datalake.crn", sdxCrn);
        attributes.put("databus.header.datalake.name", sdxContextName);
        wa.setAttributes(attributes);
        // WHEN
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, sdxContextName, sdxCrn, null);
        // THEN
        assertTrue(containsSafatyValveWithValue(result, "databus.header.environment.crn", "envCrn"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.environment.name", "envName"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.datalake.crn", sdxCrn));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.datalake.name", sdxContextName));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.cloudprovider.name", "AWS"));
        assertTrue(containsSafatyValveWithValue(result, "databus.header.cloudprovider.region", "us-west-1"));
    }

    @Test
    public void testBuildTelemetryConfigList() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setCluster(cluster);
        WorkloadAnalytics wa = new WorkloadAnalytics();
        // WHEN
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, null, null, null);
        // THEN
        assertEquals(1, result.getItems().size());
        assertTrue(result.getItems().get(0).getValue().contains("cluster.type=DATALAKE"));
    }

    @Test
    public void testEnrichWithSdxData() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setType(StackType.WORKLOAD);
        stack.setCluster(cluster);
        stack.setEnvironmentCrn("envCrn");
        stack.setDatalakeCrn("datalakeCrn");
        stack.setResourceCrn("datahubCrn");
        stack.setName("datahubName");
        stack.setCloudPlatform("AWS");
        stack.setRegion("us-west-1");
        Map<String, String> safetyValveMap = new HashMap<>();
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        // WHEN
        underTest.enrichWithEnvironmentMetadata(null, null, stack, workloadAnalytics, safetyValveMap);
        // THEN
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.id"));
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.name"));
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "cl1-1");
        assertEquals(safetyValveMap.get("databus.header.environment.crn"), "envCrn");
        assertEquals(safetyValveMap.get("databus.header.datalake.crn"), "datalakeCrn");
        assertEquals(safetyValveMap.get("databus.header.datahub.crn"), "datahubCrn");
        assertEquals(safetyValveMap.get("databus.header.datahub.name"), "datahubName");
        assertEquals(safetyValveMap.get("databus.header.cloudprovider.name"), "AWS");
        assertEquals(safetyValveMap.get("databus.header.cloudprovider.region"), "us-west-1");
    }

    @Test
    public void testEnrichWithSdxDataWithExistingSdxData() {
        // GIVEN
        Map<String, String> safetyValveMap = new HashMap<>();
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        // WHEN
        underTest.enrichWithEnvironmentMetadata("mySdxName", "crn:cdp:iam:us-west-1:accountId:user:mySdxId", null, workloadAnalytics, safetyValveMap);
        // THEN
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.id"));
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.name"));
        assertEquals(safetyValveMap.get("databus.header.sdx.id"), "mySdxId");
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "mySdxName");
    }

    @Test
    public void testEnrichWithSdxDataWithProvidedSdxData() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setType(StackType.WORKLOAD);
        stack.setCluster(cluster);
        stack.setEnvironmentCrn("envCrn");
        stack.setDatalakeCrn("datalakeCrn");
        stack.setResourceCrn("datahubCrn");
        stack.setName("datahubName");
        stack.setCloudPlatform("AWS");
        stack.setRegion("us-west-1");
        Map<String, String> safetyValveMap = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("databus.header.sdx.id", "mySdxId");
        attributes.put("databus.header.sdx.name", "mySdxName");
        attributes.put("databus.header.environment.crn", "envCrn");
        attributes.put("databus.header.environment.name", "envName");
        attributes.put("databus.header.datalake.crn", "datalakeCrn");
        attributes.put("databus.header.datalake.name", "datalakeName");
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        workloadAnalytics.setAttributes(attributes);
        // WHEN
        underTest.enrichWithEnvironmentMetadata(null, null, stack, workloadAnalytics, safetyValveMap);
        // THEN
        assertEquals(safetyValveMap.get("databus.header.sdx.id"), "mySdxId");
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "mySdxName");
        assertEquals(safetyValveMap.get("databus.header.sdx.id"), "mySdxId");
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "mySdxName");
        assertEquals(safetyValveMap.get("databus.header.environment.crn"), "envCrn");
        assertEquals(safetyValveMap.get("databus.header.datalake.crn"), "datalakeCrn");
        assertEquals(safetyValveMap.get("databus.header.datahub.crn"), "datahubCrn");
        assertEquals(safetyValveMap.get("databus.header.datahub.name"), "datahubName");
        assertEquals(safetyValveMap.get("databus.header.cloudprovider.name"), "AWS");
        assertEquals(safetyValveMap.get("databus.header.cloudprovider.region"), "us-west-1");
    }

    @Test
    public void testUpdateServiceMonitorConfigs() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setStackVersion("7.2.16");
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");
        Cluster cluster = new Cluster();
        cluster.setCloudbreakClusterManagerMonitoringUser("admin");
        cluster.setCloudbreakClusterManagerMonitoringPassword("admin123");
        stack.setCluster(cluster);
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl("url");
        telemetry.setMonitoring(monitoring);
        Features features = new Features();
        features.addMonitoring(true);
        telemetry.setFeatures(features);
        given(mgmtRoleConfigGroupsResourceApi.readConfig(any(), anyString())).willReturn(new ApiConfigList()
                .addItemsItem(new ApiConfig().name("prometheus_metrics_endpoint_port"))
                .addItemsItem(new ApiConfig().name("prometheus_metrics_endpoint_username"))
                .addItemsItem(new ApiConfig().name("prometheus_metrics_endpoint_password"))
                .addItemsItem(new ApiConfig().name("prometheus_adapter_enabled")));
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(EXPORTER_PORT);
        given(clouderaManagerApiFactory.getMgmtRoleConfigGroupsResourceApi(apiClient)).willReturn(mgmtRoleConfigGroupsResourceApi);
        // WHEN
        underTest.updateServiceMonitorConfigs(stack, apiClient, telemetry);
        // THEN
        verify(mgmtRoleConfigGroupsResourceApi, times(1)).readConfig(any(), anyString());
        verify(mgmtRoleConfigGroupsResourceApi, times(1)).updateConfig(any(), anyString(), any());
    }

    private boolean containsConfigWithValue(ApiConfigList configList, String configKey, String configValue) {
        return configList.getItems().stream()
                .anyMatch(c -> configKey.equals(c.getName()) && configValue.equals(c.getValue()));
    }

    private boolean containsSafatyValveWithValue(ApiConfigList configList, String configKey, String configValue) {
        String safetyValve = configList.getItems()
                .stream()
                .filter(config -> "telemetrypublisher_safety_valve".equals(config.getName()))
                .map(ApiConfig::getValue)
                .findFirst()
                .orElse("");

        return Arrays.stream(safetyValve.split("\n"))
                .map(line -> line.split("="))
                .filter(parts -> parts.length == 2)
                .anyMatch(parts -> parts[0].equals(configKey) && parts[1].equals(configValue));
    }
}
