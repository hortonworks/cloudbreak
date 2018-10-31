package com.sequenceiq.cloudbreak.converter.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.VaultService;

@Component
public class StackToAutoscaleStackResponseJsonConverter extends AbstractConversionServiceAwareConverter<Stack, AutoscaleStackResponse> {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private VaultService vaultService;

    @Override
    public AutoscaleStackResponse convert(Stack source) {
        AutoscaleStackResponse result = new AutoscaleStackResponse();
        result.setStackId(source.getId());
        result.setName(source.getName());
        result.setOwner(source.getOwner());
        result.setAccount(source.getOwner());
        result.setGatewayPort(source.getGatewayPort());
        result.setCreated(source.getCreated());
        result.setStatus(source.getStatus());

        if (source.getCluster() != null) {
            Cluster cluster = source.getCluster();
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(source);
            result.setAmbariServerIp(gatewayIp);
            result.setUserName(vaultService.resolveSingleValue(cluster.getCloudbreakAmbariUser()));
            result.setPassword(vaultService.resolveSingleValue(cluster.getCloudbreakAmbariPassword()));
            result.setClusterStatus(cluster.getStatus());
        }
        return result;
    }
}
