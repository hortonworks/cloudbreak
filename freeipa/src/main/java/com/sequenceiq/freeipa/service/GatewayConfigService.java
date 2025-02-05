package com.sequenceiq.freeipa.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltService;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
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

    @Inject
    private SaltService saltService;

    public List<GatewayConfig> getNotDeletedGatewayConfigs(Stack stack) {
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

    /**
     * Use this method to retrieve the primary gateway config for communicating with the Salt on the cluster.
     * Use {@link #getPrimaryGatewayConfig(Stack) } for other communications, e.g. access FreeIPA, etc.
     *
     * @return primary gateway config for Salt communication
     */
    public GatewayConfig getPrimaryGatewayConfigForSalt(Stack stack) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
        List<GatewayConfig> gatewayConfigs = getGatewayConfigs(stack, instanceMetaDatas);
        try {
            return saltService.getPrimaryGatewayConfig(gatewayConfigs);
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new CloudbreakRuntimeException(e);
        }
    }

    /**
     * Use this method to retrieve the primary gateway config for communicating with FreeIPA, etc. (except Salt)
     * Use {@link #getPrimaryGatewayConfigForSalt(Stack) } to communicate with Salt on the cluster
     *
     * @return primary gateway config
     */
    public GatewayConfig getPrimaryGatewayConfig(Stack stack) {
        return getGatewayConfig(stack, getPrimaryGwInstance(stack));

    }

    private InstanceMetaData getPrimaryGwInstance(Stack stack) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
        return getPrimaryGwInstance(instanceMetaDatas);
    }

    private InstanceMetaData getPrimaryGwInstance(Collection<InstanceMetaData> instanceMetaDatas) {
        return instanceMetaDatas.stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType()))
                .findFirst().orElseThrow(() -> new NotFoundException("Gateway instance is not found"));
    }

    public GatewayConfig getGatewayConfig(Stack stack, InstanceMetaData gatewayInstance) {
        return tlsSecurityService.buildGatewayConfig(stack, gatewayInstance, getSaltClientConfig(stack), false);
    }

    private SaltClientConfig getSaltClientConfig(Stack stack) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String privateKey = saltSecurityConfig.getSaltBootSignPrivateKeyVault();
        String saltBootPassword = saltSecurityConfig.getSaltBootPasswordVault();
        String saltPassword = saltSecurityConfig.getSaltPasswordVault();
        return new SaltClientConfig(saltPassword, saltBootPassword, new String(Base64.decodeBase64(privateKey)));
    }
}
