package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Service
public class GatewayConfigService {

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<GatewayConfig> getAllGatewayConfigs(Stack stack) {
        List<GatewayConfig> result = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getGatewayInstanceMetadata()) {
            result.add(getGatewayConfig(stack, instanceMetaData, stack.getCluster().getGateway().getEnableGateway()));
        }
        return result;
    }

    public GatewayConfig getPrimaryGatewayConfig(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        if (gatewayInstance == null) {
            throw new NotFoundException("Gateway instance does not found");
        }
        return getGatewayConfig(stack, gatewayInstance, stack.getCluster().getGateway().getEnableGateway());
    }

    public GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance, Boolean knoxGatewayEnabled) {
        return tlsSecurityService.buildGatewayConfig(stack.getId(), gatewayInstance, stack.getGatewayPort(), getSaltClientConfig(stack), knoxGatewayEnabled);
    }

    public String getPrimaryGatewayIp(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        return gatewayInstance == null ? null : getGatewayIp(stack, gatewayInstance);
    }

    public String getGatewayIp(Stack stack, InstanceMetaData gatewayInstance) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (stack.getSecurityConfig().usePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    private SaltClientConfig getSaltClientConfig(Stack stack) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        return new SaltClientConfig(securityConfig.getSaltPassword(), securityConfig.getSaltBootPassword(), securityConfig.getCloudbreakSshPrivateKeyDecoded());
    }
}
