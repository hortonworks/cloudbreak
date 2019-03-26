package com.sequenceiq.cloudbreak.converter.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class AutoscaleStackToAutoscaleStackResponseJsonConverter extends AbstractConversionServiceAwareConverter<AutoscaleStack, AutoscaleStackResponse> {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public AutoscaleStackResponse convert(AutoscaleStack source) {
        AutoscaleStackResponse result = new AutoscaleStackResponse();
        result.setStackId(source.getId());
        result.setName(source.getName());
        result.setOwner(source.getOwner());
        result.setAccount(source.getOwner());
        result.setGatewayPort(source.getGatewayPort());
        result.setCreated(source.getCreated());
        result.setStatus(source.getStackStatus());

        if (source.getClusterStatus() != null) {
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(source);
            result.setAmbariServerIp(gatewayIp);
            result.setUserName(source.getCloudbreakAmbariUser());
            result.setPassword(source.getCloudbreakAmbariPassword());
            result.setClusterStatus(source.getClusterStatus());
        }
        return result;
    }
}
