package com.sequenceiq.cloudbreak.service;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@Service
public class GatewayConfigService {

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<GatewayConfig> getAllGatewayConfigs(Stack stack) throws CloudbreakSecuritySetupException {
        List<GatewayConfig> result = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getGatewayInstanceGroup().getInstanceMetaData()) {
            result.add(getGatewayConfig(stack, instanceMetaData, stack.getCluster().getGateway().getEnableGateway()));
        }
        return result;
    }

    public GatewayConfig getPrimaryGatewayConfig(Stack stack) throws CloudbreakSecuritySetupException {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        return getGatewayConfig(stack, gatewayInstance, stack.getCluster().getGateway().getEnableGateway());
    }

    public GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance, Boolean knoxGatewayEnabled) throws CloudbreakSecuritySetupException {
        return tlsSecurityService.buildGatewayConfig(stack.getId(), gatewayInstance, stack.getGatewayPort(), getSaltClientConfig(stack), knoxGatewayEnabled);
    }

    public String getPrimaryGatewayIp(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        return getGatewayIp(stack, gatewayInstance);
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
        return new SaltClientConfig(securityConfig.getSaltPassword(), securityConfig.getSaltBootPassword(),
                new String(BaseEncoding.base64().decode(securityConfig.getCloudbreakSshPrivateKey())));
    }
}
