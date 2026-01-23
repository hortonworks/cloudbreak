package com.sequenceiq.cloudbreak.node.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorMeteringStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServicesStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.RetryType;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class CdpDoctorServiceTest {

    private static final String HOSTNAME = "host1.example.com";

    private static final String PRIVATE_IP = "10.0.0.10";

    private static final String PUBLIC_IP = "52.10.10.10";

    private static final String INSTANCE_ID = "i-123456";

    private static final String VERSION_VALUE = "1.2.3";

    private static final String METERING_CMD = "cdp-doctor metering status --format json";

    private static final String SERVICES_CMD = "cdp-doctor service status --format json";

    private static final String VALID_METERING_JSON_WITH_NULL = "{\"heartbeatAgentRunning\": null}";

    private static final String VALID_METERING_JSON_OK = "{\"heartbeatAgentRunning\": \"OK\"}";

    private static final String VALID_NETWORK_JSON_WITH_NULL = "{\"databusAccessible\": null}";

    private static final String VALID_NETWORK_JSON_OK = "{\"databusAccessible\": \"OK\"}";

    private static final String VALID_SERVICES_JSON_WITH_NULL = "{\"cmServices\": null}";

    private static final String VALID_SERVICES_JSON_WITH_LIST = "{\"cmServices\": [{\"name\":\"svc1\",\"status\":\"OK\"}]}";

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private ClusterView clusterView;

    @Mock
    private InstanceMetadataView im1;

    @Mock
    private InstanceMetadataView im2;

    @Mock
    private InstanceMetadataView im3;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private CdpDoctorService underTest;

    @Test
    @DisplayName("getTelemetryVersion returns version string when present in result map")
    void testGetTelemetryVersionSuccess() throws Exception {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfig.getPrivateAddress()).thenReturn(PRIVATE_IP);
        when(gatewayConfig.getPublicAddress()).thenReturn(PUBLIC_IP);
        when(gatewayConfig.getInstanceId()).thenReturn(INSTANCE_ID);
        when(saltOrchestrator.runShellCommandOnNodes(any())).thenReturn(Map.of(HOSTNAME, VERSION_VALUE, "otherHost", "0.0.1"));

        String result = underTest.getTelemetryVersion(stack);

        assertThat(result).isEqualTo(VERSION_VALUE);
        ArgumentCaptor<OrchestratorRunParams> paramsCaptor = ArgumentCaptor.forClass(OrchestratorRunParams.class);
        verify(saltOrchestrator).runShellCommandOnNodes(paramsCaptor.capture());
        OrchestratorRunParams captured = paramsCaptor.getValue();
        assertThat(captured.command()).isEqualTo("rpm -q --queryformat '%-{VERSION}' \"cdp-telemetry\"");
        assertThat(captured.errorMessage()).isEqualTo("Failed to get telemetry version");
        assertThat(captured.gatewayConfigs()).hasSize(1).first().isSameAs(gatewayConfig);
        assertThat(captured.nodes()).hasSize(1);
        Node node = captured.nodes().iterator().next();
        assertThat(node.getHostname()).isEqualTo(HOSTNAME);
        assertThat(node.getPrivateIp()).isEqualTo(PRIVATE_IP);
        assertThat(node.getPublicIp()).isEqualTo(PUBLIC_IP);
        assertThat(node.getInstanceId()).isEqualTo(INSTANCE_ID);
    }

    @Test
    @DisplayName("getTelemetryVersion throws when hostname key missing in result map")
    void testGetTelemetryVersionMissingHostKey() {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfig.getPrivateAddress()).thenReturn(PRIVATE_IP);
        when(gatewayConfig.getPublicAddress()).thenReturn(PUBLIC_IP);
        when(gatewayConfig.getInstanceId()).thenReturn(INSTANCE_ID);
        when(saltOrchestrator.runShellCommandOnNodes(any())).thenReturn(Map.of("differentHost", VERSION_VALUE));

        CloudbreakOrchestratorFailedException ex = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.getTelemetryVersion(stack));
        assertThat(ex.getMessage()).contains("Telemetry version is not available on host: " + HOSTNAME);
    }

    @Test
    @DisplayName("getTelemetryVersion throws when result map empty")
    void testGetTelemetryVersionEmptyResult() {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(gatewayConfig.getHostname()).thenReturn(HOSTNAME);
        when(gatewayConfig.getPrivateAddress()).thenReturn(PRIVATE_IP);
        when(gatewayConfig.getPublicAddress()).thenReturn(PUBLIC_IP);
        when(gatewayConfig.getInstanceId()).thenReturn(INSTANCE_ID);
        when(saltOrchestrator.runShellCommandOnNodes(any())).thenReturn(Map.of());

        CloudbreakOrchestratorFailedException ex = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.getTelemetryVersion(stack));
        assertThat(ex.getMessage()).contains("Telemetry version is not available on host: " + HOSTNAME);
    }

    @Test
    @DisplayName("getMeteringStatusForMinions filters invalid JSON and preserves defaults on null values")
    void testGetMeteringStatusForMinions() throws Exception {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(saltOrchestrator.runCommandOnAllHosts(gatewayConfig, METERING_CMD, RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES))
                .thenReturn(Map.of(
                        "host1", VALID_METERING_JSON_WITH_NULL,
                        "host2", "not-json",
                        "host3", VALID_METERING_JSON_OK));

        Map<String, CdpDoctorMeteringStatusResponse> result = underTest.getMeteringStatusForMinions(stack);

        assertThat(result).hasSize(2).containsKeys("host1", "host3");
        assertThat(result.get("host1").getHeartbeatAgentRunning()).isEqualTo(CdpDoctorCheckStatus.UNKNOWN);
        assertThat(result.get("host3").getHeartbeatAgentRunning()).isEqualTo(CdpDoctorCheckStatus.OK);
        verify(saltOrchestrator).runCommandOnAllHosts(gatewayConfig, METERING_CMD, RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES);
    }

    @Test
    @DisplayName("getMeteringStatusForMinions returns empty map when all invalid")
    void testGetMeteringStatusForMinionsAllInvalid() throws Exception {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(saltOrchestrator.runCommandOnAllHosts(gatewayConfig, METERING_CMD, RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES))
                .thenReturn(Map.of(
                        "h1", "invalid",
                        "h2", "also-invalid"));

        Map<String, CdpDoctorMeteringStatusResponse> result = underTest.getMeteringStatusForMinions(stack);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getNetworkStatusForMinions filters invalid JSON and preserves defaults on null values")
    void testGetNetworkStatusForMinions() throws Exception {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(stack.getAllAvailableInstances()).thenReturn(List.of());
        when(saltOrchestrator.runCommandOnHosts(any(), any(), any(), any()))
                .thenReturn(Map.of(
                        "hostA", VALID_NETWORK_JSON_WITH_NULL,
                        "hostB", "garbage",
                        "hostC", VALID_NETWORK_JSON_OK));

        Map<String, CdpDoctorNetworkStatusResponse> result = underTest.getNetworkStatusForMinions(stack);
        assertThat(result).hasSize(2).containsKeys("hostA", "hostC");
        assertThat(result.get("hostA").getDatabusAccessible()).isEqualTo(CdpDoctorCheckStatus.UNKNOWN);
        assertThat(result.get("hostC").getDatabusAccessible()).isEqualTo(CdpDoctorCheckStatus.OK);
    }

    @Test
    @DisplayName("getServicesStatusForMinions parses cmServices list and ignores null lists")
    void testGetServicesStatusForMinions() throws Exception {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(saltOrchestrator.runCommandOnAllHosts(gatewayConfig, SERVICES_CMD, RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES))
                .thenReturn(Map.of(
                        "h1", VALID_SERVICES_JSON_WITH_NULL,
                        "h2", "randomInvalid",
                        "h3", VALID_SERVICES_JSON_WITH_LIST));

        Map<String, CdpDoctorServicesStatusResponse> result = underTest.getServicesStatusForMinions(stack);
        assertThat(result).hasSize(2).containsKeys("h1", "h3");
        assertThat(result.get("h1").getCmServices()).isEmpty();
        assertThat(result.get("h3").getCmServices()).hasSize(1);
        assertThat(result.get("h3").getCmServices().getFirst().getStatus()).isEqualTo(CdpDoctorCheckStatus.OK);
        verify(saltOrchestrator).runCommandOnAllHosts(gatewayConfig, SERVICES_CMD, RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES);
    }

    private void mockGatewayAndResult(Set<String> expectedTargets) throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        Map<String, String> rawResult = expectedTargets.stream().collect(java.util.stream.Collectors.toMap(h -> h, h -> "{}"));
        when(saltOrchestrator.runCommandOnHosts(any(), any(), any(), any()))
                .thenReturn(rawResult);
    }

    @Test
    @DisplayName("New instances present -> only new instances targeted")
    void onlyNewInstancesTargeted() throws Exception {
        when(stack.getCluster()).thenReturn(clusterView);
        when(clusterView.getCreationFinished()).thenReturn(1500L);
        when(im1.getStartDate()).thenReturn(1000L);
        when(im2.getStartDate()).thenReturn(2000L);
        when(im3.getStartDate()).thenReturn(1200L);
        when(im2.getDiscoveryFQDN()).thenReturn("host2.example.com");
        when(stack.getAllAvailableInstances()).thenReturn(List.of(im1, im2, im3));
        Set<String> expectedTargets = Set.of("host2.example.com");
        mockGatewayAndResult(expectedTargets);

        Map<String, CdpDoctorNetworkStatusResponse> result = underTest.getNetworkStatusForMinions(stack);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("host2.example.com"));
    }

    @Test
    @DisplayName("No new instances -> all instances targeted")
    void noNewInstancesAllTargeted() throws Exception {
        when(stack.getCluster()).thenReturn(clusterView);
        when(clusterView.getCreationFinished()).thenReturn(2000L);
        when(im1.getStartDate()).thenReturn(1000L);
        when(im2.getStartDate()).thenReturn(1500L);
        when(im3.getStartDate()).thenReturn(1800L);
        when(im1.getDiscoveryFQDN()).thenReturn("host1.example.com");
        when(im2.getDiscoveryFQDN()).thenReturn("host2.example.com");
        when(im3.getDiscoveryFQDN()).thenReturn("host3.example.com");
        when(stack.getAllAvailableInstances()).thenReturn(List.of(im1, im2, im3));
        Set<String> expectedTargets = Set.of("host1.example.com", "host2.example.com", "host3.example.com");
        mockGatewayAndResult(expectedTargets);

        Map<String, CdpDoctorNetworkStatusResponse> result = underTest.getNetworkStatusForMinions(stack);
        assertEquals(3, result.size());
        assertTrue(result.keySet().containsAll(expectedTargets));
    }

    @Test
    @DisplayName("Cluster null -> creationFinished null -> all instances considered new")
    void clusterNullAllConsideredNew() throws Exception {
        when(stack.getCluster()).thenReturn(null);
        when(im1.getStartDate()).thenReturn(1000L);
        when(im2.getStartDate()).thenReturn(2000L);
        when(im3.getStartDate()).thenReturn(null);
        when(im1.getDiscoveryFQDN()).thenReturn("host1.example.com");
        when(im2.getDiscoveryFQDN()).thenReturn("host2.example.com");
        when(stack.getAllAvailableInstances()).thenReturn(List.of(im1, im2, im3));
        Set<String> expectedTargets = Set.of("host1.example.com", "host2.example.com");
        mockGatewayAndResult(expectedTargets);

        Map<String, CdpDoctorNetworkStatusResponse> result = underTest.getNetworkStatusForMinions(stack);
        assertEquals(2, result.size());
        assertTrue(result.keySet().containsAll(expectedTargets));
    }

    @Test
    @DisplayName("Instance start equals creationFinished -> not new (falls back to all when none strictly greater)")
    void startEqualsCreationFinishedNotNew() throws Exception {
        when(stack.getCluster()).thenReturn(clusterView);
        when(clusterView.getCreationFinished()).thenReturn(1500L);
        when(im1.getStartDate()).thenReturn(1500L);
        when(im2.getStartDate()).thenReturn(1400L);
        when(im3.getStartDate()).thenReturn(null);
        when(im1.getDiscoveryFQDN()).thenReturn("host1.example.com");
        when(im2.getDiscoveryFQDN()).thenReturn("host2.example.com");
        when(im3.getDiscoveryFQDN()).thenReturn("host3.example.com");
        when(stack.getAllAvailableInstances()).thenReturn(List.of(im1, im2, im3));
        Set<String> expectedTargets = Set.of("host1.example.com", "host2.example.com", "host3.example.com");
        mockGatewayAndResult(expectedTargets);

        Map<String, CdpDoctorNetworkStatusResponse> result = underTest.getNetworkStatusForMinions(stack);
        assertEquals(3, result.size());
        assertTrue(result.keySet().containsAll(expectedTargets));
    }
}

