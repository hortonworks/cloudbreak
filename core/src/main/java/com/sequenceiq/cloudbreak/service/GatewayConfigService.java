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
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class GatewayConfigService {

    @Inject
    private TlsSecurityService tlsSecurityService;

    public List<GatewayConfig> getAllGatewayConfigs(StackDtoDelegate stack) {
        boolean knoxGatewayEnabled = stack.hasGateway();
        List<InstanceMetadataView> reachableGatewayInstanceMetadata = stack.getReachableGatewayInstanceMetadata();
        if (reachableGatewayInstanceMetadata.isEmpty()) {
            throw new NotFoundException("No reachable gateway found");
        } else {
            return reachableGatewayInstanceMetadata.stream()
                    .map(im -> getGatewayConfig(stack.getStack(), stack.getSecurityConfig(), im, knoxGatewayEnabled))
                    .collect(Collectors.toList());
        }
    }

    public GatewayConfig getPrimaryGatewayConfig(StackDtoDelegate stack) {
        InstanceMetadataView gatewayInstance = stack.getPrimaryGatewayInstance();
        if (gatewayInstance == null) {
            throw new NotFoundException("Gateway instance does not found");
        }
        return getGatewayConfig(stack.getStack(), stack.getSecurityConfig(), gatewayInstance, stack.hasGateway());
    }

    public GatewayConfig getPrimaryGatewayConfig(StackDto stackDto) {
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        if (gatewayInstance == null) {
            throw new NotFoundException("Gateway instance does not found");
        }
        return getGatewayConfig(stackDto.getStack(), stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
    }

    public GatewayConfig getGatewayConfig(StackView stack, SecurityConfig securityConfig, InstanceMetadataView gatewayInstance,
            Boolean knoxGatewayEnabled) {
        return tlsSecurityService.buildGatewayConfig(stack, gatewayInstance, stack.getGatewayPort(),
                getSaltClientConfig(securityConfig), knoxGatewayEnabled);
    }

    public String getPrimaryGatewayIp(StackDtoDelegate stack) {
        InstanceMetadataView gatewayInstance = stack.getPrimaryGatewayInstance();
        return gatewayInstance == null ? null : getGatewayIp(stack.getSecurityConfig(), gatewayInstance);
    }

    public String getPrimaryGatewayIp(AutoscaleStack stack) {
        String gatewayIP = Optional.ofNullable(stack.getPublicIp()).orElse(stack.getPrivateIp());
        if (stack.getUsePrivateIpToTls()) {
            gatewayIP = stack.getPrivateIp();
        }
        return gatewayIP;
    }

    public String getGatewayIp(SecurityConfig securityConfig, InstanceMetadataView gatewayInstance) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (securityConfig.isUsePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    private SaltClientConfig getSaltClientConfig(SecurityConfig securityConfig) {
        if (securityConfig != null) {
            SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
            String privateKey = saltSecurityConfig.getSaltBootSignPrivateKey();
            String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
            String saltPassword = saltSecurityConfig.getSaltPassword();
            return new SaltClientConfig(saltPassword, saltBootPassword, new String(Base64.decodeBase64(privateKey)));
        } else {
            throw new NotFoundException("Cannot find security config for the given gateway instance.");
        }
    }
}
