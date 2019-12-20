package com.sequenceiq.cloudbreak.converter.v4.stacks;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class StackToAutoscaleStackV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, AutoscaleStackV4Response> {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public AutoscaleStackV4Response convert(Stack source) {
        AutoscaleStackV4Response result = new AutoscaleStackV4Response();
        result.setTenant(source.getWorkspace().getTenant().getName());
        result.setWorkspaceId(source.getWorkspace().getId());
        result.setUserId(source.getCreator().getUserId());
        result.setStackId(source.getId());
        result.setName(source.getName());
        result.setGatewayPort(source.getGatewayPort());
        result.setCreated(source.getCreated());
        result.setStatus(source.getStatus());
        result.setStackCrn(source.getResourceCrn());
        result.setTunnel(source.getTunnel());

        if (source.getCluster() != null) {
            Cluster cluster = source.getCluster();
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(source);
            result.setAmbariServerIp(gatewayIp);
            result.setUserNamePath(cluster.getCloudbreakAmbariUserSecret());
            result.setPasswordPath(cluster.getCloudbreakAmbariPasswordSecret());
            result.setClusterStatus(cluster.getStatus());
        }
        return result;
    }
}
