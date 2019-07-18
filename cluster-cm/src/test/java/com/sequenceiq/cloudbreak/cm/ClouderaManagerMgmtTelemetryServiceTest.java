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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;

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
        WorkloadAnalytics wa = new WorkloadAnalytics();
        Telemetry telemetry = new Telemetry(null, wa);
        ApiConfigList apiConfigList = new ApiConfigList();
        ApiResponse response = new ApiResponse<>(0, null, apiConfigList);
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(apiClient.execute(any(), any())).thenReturn(response);
        when(clouderaManagerDatabusService.getAltusCredential(stack)).thenReturn(credential);
        // WHEN
        underTest.setupTelemetryRole(stack, apiClient, null, new ApiRoleList(), telemetry);
        // THEN
        verify(apiClient, times(1)).execute(any(), any());
        verify(externalAccountService, times(1)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
        verify(clouderaManagerDatabusService, times(1)).getAltusCredential(stack);
    }

    @Test
    public void testSetupTelemetryWaDisabled() throws ApiException {
        // GIVEN
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        Telemetry telemetry = new Telemetry(null, null);
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
        WorkloadAnalytics wa = new WorkloadAnalytics();
        Telemetry telemetry = new Telemetry(null, wa);
        // WHEN
        underTest.setupTelemetryRole(stack, null, null, null, telemetry);
        // THEN
        verify(externalAccountService, times(0)).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
    }

    @Test
    public void testBuildTelemetryCMConfigList() {
        // GIVEN
        // WHEN
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        ApiConfigList result = underTest.buildTelemetryCMConfigList(workloadAnalytics);
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
        ApiConfigList result = underTest.buildTelemetryCMConfigList(workloadAnalytics);
        // THEN
        assertEquals(5, result.getItems().size());
        assertTrue(containsConfigWithValue(result, "telemetry_altus_url", "customEndpoint"));
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
        ApiConfigList result = underTest.buildTelemetryConfigList(stack, wa, null, null);
        // THEN
        assertEquals(1, result.getItems().size());
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
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        // WHEN
        underTest.enrichWithSdxData(null, null, stack, workloadAnalytics, safetyValveMap);
        // THEN
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.id"));
        assertTrue(safetyValveMap.containsKey("databus.header.sdx.name"));
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "cl1-1");
    }

    @Test
    public void testEnrichWithSdxDataWithExistingSdxData() {
        // GIVEN
        Map<String, String> safetyValveMap = new HashMap<>();
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        // WHEN
        underTest.enrichWithSdxData("mySdxName", "crn:altus:iam:us-west-1:accountId:user:mySdxId", null, workloadAnalytics, safetyValveMap);
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
        stack.setCluster(cluster);
        Map<String, String> safetyValveMap = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("databus.header.sdx.id", "mySdxId");
        attributes.put("databus.header.sdx.name", "mySdxName");
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint("customEndpoint");
        workloadAnalytics.setAttributes(attributes);
        // WHEN
        underTest.enrichWithSdxData(null, null, stack, workloadAnalytics, safetyValveMap);
        // THEN
        assertEquals(safetyValveMap.get("databus.header.sdx.id"), "mySdxId");
        assertEquals(safetyValveMap.get("databus.header.sdx.name"), "mySdxName");
    }

    private boolean containsConfigWithValue(ApiConfigList configList, String configKey, String configValue) {
        return configList.getItems().stream()
                .anyMatch(c -> configKey.equals(c.getName()) && configValue.equals(c.getValue()));
    }
}