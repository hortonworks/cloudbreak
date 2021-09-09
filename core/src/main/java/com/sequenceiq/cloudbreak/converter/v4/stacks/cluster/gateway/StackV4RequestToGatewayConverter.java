package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class StackV4RequestToGatewayConverter {

    @Inject
    private GatewayConvertUtil convertUtil;

    @Inject
    private GatewayV4RequestValidator gatewayJsonValidator;

    public Gateway convert(StackV4Request source) {
        Gateway gateway = new Gateway();
        GatewayV4Request gatewayJson = source.getCluster().getGateway();
        ValidationResult validationResult = gatewayJsonValidator.validate(gatewayJson);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        convertUtil.setBasicProperties(gatewayJson, gateway);
        convertUtil.setTopologies(gatewayJson, gateway);
        convertUtil.setGatewayPathAndSsoProvider(gatewayJson, gateway);
        return gateway;
    }

}
