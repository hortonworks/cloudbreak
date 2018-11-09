package com.sequenceiq.cloudbreak.service;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@Service
public class GatewayConfigService {

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<GatewayConfig> getAllGatewayConfigs(Stack stack) {
        List<GatewayConfig> result = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getGatewayInstanceMetadata()) {
            result.add(getGatewayConfig(stack, instanceMetaData, stack.getCluster().hasGateway()));
        }
        return result;
    }

    public GatewayConfig getPrimaryGatewayConfig(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        if (gatewayInstance == null) {
            throw new NotFoundException("Gateway instance does not found");
        }
        return getGatewayConfig(stack, gatewayInstance, stack.getCluster().hasGateway());
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
        if (stack.getSecurityConfig().isUsePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    private SaltClientConfig getSaltClientConfig(Stack stack) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String privateKey = saltSecurityConfig.getSaltBootSignPrivateKey().getRaw();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword().getRaw();
        String saltPassword = saltSecurityConfig.getSaltPassword().getRaw();
        return new SaltClientConfig(saltPassword, saltBootPassword, new String(Base64.decodeBase64(privateKey)));
    }
}
