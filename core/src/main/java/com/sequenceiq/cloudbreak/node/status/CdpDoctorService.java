package com.sequenceiq.cloudbreak.node.status;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorMeteringStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorServicesStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Service
public class CdpDoctorService {

    private static final String DOCTOR_NETWORK_COMMAND = "cdp-doctor network status --format json";

    private static final String DOCTOR_METERING_COMMAND = "cdp-doctor metering status --format json";

    private static final String DOCTOR_SERVICES_COMMAND = "cdp-doctor service status --format json";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    public Map<String, CdpDoctorMeteringStatusResponse> getMeteringStatusForMinions(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return saltOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, DOCTOR_METERING_COMMAND).entrySet().stream()
                .filter(entry -> JsonUtil.isValid(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtil.readValueUnchecked(entry.getValue(), CdpDoctorMeteringStatusResponse.class)));
    }

    public Map<String, CdpDoctorNetworkStatusResponse> getNetworkStatusForMinions(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return saltOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, DOCTOR_NETWORK_COMMAND).entrySet().stream()
                .filter(entry -> JsonUtil.isValid(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtil.readValueUnchecked(entry.getValue(), CdpDoctorNetworkStatusResponse.class)));
    }

    public Map<String, CdpDoctorServicesStatusResponse> getServicesStatusForMinions(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return saltOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, DOCTOR_SERVICES_COMMAND).entrySet().stream()
                .filter(entry -> JsonUtil.isValid(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtil.readValueUnchecked(entry.getValue(), CdpDoctorServicesStatusResponse.class)));
    }
}
