package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.client.ApiResponse;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalytics;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class ClouderaManagerMgmtTelemetryServiceTest {

    @InjectMocks
    private ClouderaManagerMgmtTelemetryService underTest;

    @Mock
    private ClouderaManagerExternalAccountService externalAccountService;

    @Mock
    private ClouderaManagerDatabusService clouderaManagerDatabusService;

    @Mock
    private ApiClient apiClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetupTelemetry() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, null, null, null);
        Telemetry telemetry = new Telemetry(null, wa);
        ApiConfigList apiConfigList = new ApiConfigList();
        ApiResponse response = new ApiResponse<>(0, null, apiConfigList);
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(apiClient.execute(any(), any())).thenReturn(response);
        when(clouderaManagerDatabusService.createMachineUserAndGenerateKeys(stack)).thenReturn(credential);
        // WHEN
        underTest.setupTelemetryRole(stack, apiClient, null, new ApiRoleList(), telemetry);
        // THEN
        verify(apiClient, times(1)).execute(any(), any());
        verify(externalAccountService, times(1)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
        verify(clouderaManagerDatabusService, times(1)).createMachineUserAndGenerateKeys(stack);
    }

    @Test
    public void testSetupTelemetryWaDisabled() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        WorkloadAnalytics wa = new WorkloadAnalytics(false, null, null, null, null);
        Telemetry telemetry = new Telemetry(null, wa);
        // WHEN
        underTest.setupTelemetryRole(stack, null, null, null, telemetry);
        // THEN
        verify(externalAccountService, times(0)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
    }

    @Test
    public void testSetupTelemetryForDatalake() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, null, null, null);
        Telemetry telemetry = new Telemetry(null, wa);
        // WHEN
        underTest.setupTelemetryRole(stack, null, null, null, telemetry);
        // THEN
        verify(externalAccountService, times(0)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
    }

    @Test
    public void testGetAltusCredential() {
        // GIVEN
        Stack stack = new Stack();
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, null, null, null);
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(clouderaManagerDatabusService.createMachineUserAndGenerateKeys(stack)).thenReturn(credential);
        // WHEN
        AltusCredential result = underTest.getAltusCredential(stack, wa);
        // THEN
        assertEquals("secretKey", new String(result.getPrivateKey()));
    }

    @Test
    public void testGetAltusCredentialWithProvidedKeys() {
        // GIVEN
        Stack stack = new Stack();
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, "customAccess", "customSecret", null);
        // WHEN
        AltusCredential result = underTest.getAltusCredential(stack, wa);
        // THEN
        assertEquals("customSecret", new String(result.getPrivateKey()));
    }

    @Test
    public void testTrimAndReplace() {
        // GIVEN
        String rawPrivateKey = "BEGIN\nline1\nline2\nlastline";
        // WHEN
        String result = underTest.trimAndReplacePrivateKey(rawPrivateKey.toCharArray());
        // THEN
        assertEquals("BEGIN\\nline1\\nline2\\nlastline", result);
    }

    @Test
    public void testBuildTelemetryCMConfigList() {
        // GIVEN
        // WHEN
        ApiConfigList result = underTest.buildTelemetryCMConfigList();
        // THEN
        assertEquals(4, result.getItems().size());
        assertTrue(containsConfigWithValue(result, "telemetry_wa", "true"));
        assertTrue(containsConfigWithValue(result, "telemetry_master", "true"));
        assertTrue(containsConfigWithValue(result, "telemetry_altus_account", "cb-altus-access"));
    }

    @Test
    public void testBuildTelemetryConfigList() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setCluster(cluster);
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, null, null, null);
        // WHEN
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, null);
        // THEN
        assertEquals(1, result.getItems().size());
    }

    @Test
    public void testBuildTelemetryConfigListWithCustomEndpoint() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setCluster(cluster);
        WorkloadAnalytics wa = new WorkloadAnalytics(true, "customEndpoint", null, null, null);
        // WHEN
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, null);
        // THEN
        assertEquals(2, result.getItems().size());
        assertTrue(containsConfigWithValue(result, "telemetry_altus_url", "customEndpoint"));
    }

    @Test
    public void testEnrichWithSdxData() {
        // GIVEN
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cl1");
        stack.setCluster(cluster);
        Map<String, String> safetyValveMap = new HashMap<>();
        WorkloadAnalytics wa = new WorkloadAnalytics(true, "customEndpoint", null, null, null);
        // WHEN
        underTest.enrichWithSdxData(null, stack, wa, safetyValveMap);
        // THEN
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.id"));
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.name"));
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "cl1-1");
    }

    @Test
    public void testEnrichWithSdxDataWithProvidedSdxData() {
        // GIVEN
        Map<String, String> safetyValveMap = new HashMap<>();
        Map<String, Object> sdxDataMap = new HashMap<>();
        sdxDataMap.put("sdxId", "mySdxId");
        sdxDataMap.put("sdxName", "mySdxName");
        WorkloadAnalytics wa = new WorkloadAnalytics(true, "customEndpoint", null, null, sdxDataMap);
        // WHEN
        underTest.enrichWithSdxData(null, null, wa, safetyValveMap);
        // THEN
        assertEquals(safetyValveMap.get("databus.header.sdx.id"), "mySdxId");
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "mySdxName");
    }

    private boolean containsConfigWithValue(ApiConfigList configList, String configKey, String configValue) {
        return configList.getItems().stream()
                .anyMatch(c -> configKey.equals(c.getName()) && configValue.equals(c.getValue()));
    }
}
