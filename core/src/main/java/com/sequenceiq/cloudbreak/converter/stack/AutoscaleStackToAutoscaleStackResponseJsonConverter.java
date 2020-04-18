package com.sequenceiq.cloudbreak.converter.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class AutoscaleStackToAutoscaleStackResponseJsonConverter extends AbstractConversionServiceAwareConverter<AutoscaleStack, AutoscaleStackV4Response> {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public AutoscaleStackV4Response convert(AutoscaleStack source) {
        AutoscaleStackV4Response result = new AutoscaleStackV4Response();
        result.setTenant(source.getTenantName());
        result.setWorkspaceId(source.getWorkspaceId());
        result.setUserId(source.getUserId());
        result.setUserCrn(source.getUserCrn());
        result.setStackId(source.getId());
        result.setName(source.getName());
        result.setGatewayPort(source.getGatewayPort());
        result.setCreated(source.getCreated());
        result.setStatus(source.getStackStatus());
        result.setStackCrn(source.getCrn());
        result.setCloudPlatform(source.getCloudPlatform());
        result.setStackType(source.getType());
        result.setTunnel(source.getTunnel());

        if (source.getClusterStatus() != null) {
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(source);
            result.setAmbariServerIp(gatewayIp);
            result.setUserNamePath(source.getCloudbreakAmbariUser().getSecret());
            result.setPasswordPath(source.getCloudbreakAmbariPassword().getSecret());
            result.setClusterStatus(source.getClusterStatus());
            result.setClusterManagerVariant(source.getClusterManagerVariant());
        }
        return result;
    }
}
