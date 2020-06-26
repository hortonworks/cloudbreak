package com.sequenceiq.freeipa.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
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

    public List<GatewayConfig> getNotTerminatedGatewayConfigs(Stack stack) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
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
        return getPrimaryGwInstance(instanceMetaDatas);
    }

    public InstanceMetaData getPrimaryGwInstance(Collection<InstanceMetaData> instanceMetaDatas) {
        return instanceMetaDatas.stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType()))
                .findFirst().orElseThrow(() -> new NotFoundException("Gateway instance does not found"));
    }

    private GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance) {
        return tlsSecurityService.buildGatewayConfig(stack, gatewayInstance, getSaltClientConfig(stack), false);
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
