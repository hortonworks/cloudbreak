package com.sequenceiq.cloudbreak.service;

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

    public GatewayConfig getGatewayConfig(Stack stack) throws CloudbreakSecuritySetupException {
        InstanceMetaData gatewayInstance = stack.getGatewayInstance();
        return getGatewayConfig(stack, gatewayInstance, stack.getCluster().getGateway().getEnableGateway());
    }

    public GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance, Boolean knoxGatewayEnabled)
            throws CloudbreakSecuritySetupException {
        return tlsSecurityService.buildGatewayConfig(stack.getId(), getGatewayIp(stack, gatewayInstance), gatewayInstance,
                stack.getGatewayPort(), getSaltClientConfig(stack), knoxGatewayEnabled);
    }

    public String getGatewayIp(Stack stack) {
        InstanceMetaData gatewayInstance = stack.getGatewayInstance();
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
