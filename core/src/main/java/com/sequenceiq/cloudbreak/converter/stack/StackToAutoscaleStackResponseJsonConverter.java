package com.sequenceiq.cloudbreak.converter.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class StackToAutoscaleStackResponseJsonConverter extends AbstractConversionServiceAwareConverter<Stack, AutoscaleStackResponse> {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public AutoscaleStackResponse convert(Stack source) {
        AutoscaleStackResponse result = new AutoscaleStackResponse();
        result.setTenant(source.getWorkspace().getTenant().getName());
        result.setWorkspaceId(source.getWorkspace().getId());
        result.setUserId(source.getCreator().getUserId());
        result.setStackId(source.getId());
        result.setName(source.getName());
        result.setGatewayPort(source.getGatewayPort());
        result.setCreated(source.getCreated());
        result.setStatus(source.getStatus());

        if (source.getCluster() != null) {
            Cluster cluster = source.getCluster();
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(source);
            result.setAmbariServerIp(gatewayIp);
            result.setUserName(cluster.getCloudbreakAmbariUser());
            result.setPassword(cluster.getCloudbreakAmbariPassword());
            result.setClusterStatus(cluster.getStatus());
        }
        return result;
    }
}
