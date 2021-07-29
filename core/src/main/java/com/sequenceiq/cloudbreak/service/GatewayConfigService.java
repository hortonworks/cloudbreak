package com.sequenceiq.cloudbreak.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class GatewayConfigService {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<GatewayConfig> getAllGatewayConfigs(Stack stack) {
        boolean knoxGatewayEnabled = stack.getCluster().hasGateway();
        List<InstanceMetaData> reachableGatewayInstanceMetadata = stack.getReachableGatewayInstanceMetadata();
        if (reachableGatewayInstanceMetadata.isEmpty()) {
            throw new NotFoundException("No reachable gateway found");
        } else {
            return reachableGatewayInstanceMetadata.stream()
                    .map(im -> getGatewayConfig(stack, im, knoxGatewayEnabled))
                    .collect(Collectors.toList());
        }
    }

    public GatewayConfig getPrimaryGatewayConfigWithoutLists(Stack stack) {
        Optional<InstanceMetaData> gatewayInstance = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stack.getId());
        if (gatewayInstance.isEmpty()) {
            throw new NotFoundException("Gateway instance does not found");
        }
        return getGatewayConfig(stack, gatewayInstance.get(), stack.getCluster().hasGateway());
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

    public String getPrimaryGatewayIp(AutoscaleStack stack) {
        String gatewayIP = Optional.ofNullable(stack.getPublicIp()).orElse(stack.getPrivateIp());
        if (stack.getUsePrivateIpToTls()) {
            gatewayIP = stack.getPrivateIp();
        }
        return gatewayIP;
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
