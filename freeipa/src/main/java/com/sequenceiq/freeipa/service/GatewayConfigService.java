package com.sequenceiq.freeipa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;

@Service
public class GatewayConfigService {

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public List<GatewayConfig> getAllGatewayConfigs(Stack stack) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findAllInStack(stack.getId());
        return getGatewayConfigs(stack, instanceMetaDatas);
    }

    public List<GatewayConfig> getGatewayConfigs(Stack stack, Set<InstanceMetaData> instanceMetaDatas) {
        List<GatewayConfig> result = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
            result.add(getGatewayConfig(stack, instanceMetaData));
        }
        return result;
    }

    public GatewayConfig getPrimaryGatewayConfig(Stack stack) {
        InstanceMetaData gatewayInstance = getPrimaryGwInstance(stack);
        return getGatewayConfig(stack, gatewayInstance);
    }

    private InstanceMetaData getPrimaryGwInstance(Stack stack) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findAllInStack(stack.getId());
        return instanceMetaDatas.stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType()))
                .findFirst().orElseThrow(() -> new NotFoundException("Gateway instance does not found"));
    }

    public GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance) {
        return tlsSecurityService.buildGatewayConfig(stack.getId(), gatewayInstance, stack.getGatewayport(), getSaltClientConfig(stack), false);
    }

    public String getPrimaryGatewayIp(Stack stack) {
        InstanceMetaData gatewayInstance = getPrimaryGwInstance(stack);
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
        String privateKey = saltSecurityConfig.getSaltBootSignPrivateKey();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
        String saltPassword = saltSecurityConfig.getSaltPassword();
        return new SaltClientConfig(saltPassword, saltBootPassword, new String(Base64.decodeBase64(privateKey)));
    }
}
