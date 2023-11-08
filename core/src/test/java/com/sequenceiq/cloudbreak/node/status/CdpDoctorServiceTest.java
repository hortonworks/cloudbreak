package com.sequenceiq.cloudbreak.node.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorMeteringStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServicesStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@ExtendWith(MockitoExtension.class)
public class CdpDoctorServiceTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @InjectMocks
    private CdpDoctorService cdpDoctorService;

    @Test
    void testGetMeteringReport() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any(StackDtoDelegate.class))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(saltOrchestrator.runCommandOnAllHosts(any(), any())).thenReturn(Map.of(
                "host1", "{\"random\": \"OK\", \"heartbeatAgentRunning\": null}",
                "host2", "invalidjson",
                "host3", "{\"heartbeatAgentRunning\": \"OK\"}"));

        Map<String, CdpDoctorMeteringStatusResponse> meteringStatusForMinions = cdpDoctorService.getMeteringStatusForMinions(new Stack());

        assertEquals(2, meteringStatusForMinions.size());
        assertEquals(CdpDoctorCheckStatus.UNKNOWN, meteringStatusForMinions.get("host1").getHeartbeatAgentRunning());
        assertEquals(CdpDoctorCheckStatus.OK, meteringStatusForMinions.get("host3").getHeartbeatAgentRunning());
    }

    @Test
    void testGetNetworkReport() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any(StackDtoDelegate.class))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(saltOrchestrator.runCommandOnAllHosts(any(), any())).thenReturn(Map.of(
                "host1", "{\"random\": \"OK\", \"databusAccessible\": null}",
                "host2", "invalidjson",
                "host3", "{\"databusAccessible\": \"OK\"}"));

        Map<String, CdpDoctorNetworkStatusResponse> meteringStatusForMinions = cdpDoctorService.getNetworkStatusForMinions(new Stack());

        assertEquals(2, meteringStatusForMinions.size());
        assertEquals(CdpDoctorCheckStatus.UNKNOWN, meteringStatusForMinions.get("host1").getDatabusAccessible());
        assertEquals(CdpDoctorCheckStatus.OK, meteringStatusForMinions.get("host3").getDatabusAccessible());
    }

    @Test
    void testGetServicesReport() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any(StackDtoDelegate.class))).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(saltOrchestrator.runCommandOnAllHosts(any(), any())).thenReturn(Map.of(
                "host1", "{\"random\": \"OK\", \"cmServices\": null}",
                "host2", "invalidjson",
                "host3", "{\"cmServices\": [{\"name\":\"service\",\"status\":\"OK\"}]}"));

        Map<String, CdpDoctorServicesStatusResponse> meteringStatusForMinions = cdpDoctorService.getServicesStatusForMinions(new Stack());

        assertEquals(2, meteringStatusForMinions.size());
        assertEquals(Lists.newArrayList(), meteringStatusForMinions.get("host1").getCmServices());
        assertEquals(1, meteringStatusForMinions.get("host3").getCmServices().size());
        assertEquals(CdpDoctorCheckStatus.OK, meteringStatusForMinions.get("host3").getCmServices().get(0).getStatus());
    }
}
