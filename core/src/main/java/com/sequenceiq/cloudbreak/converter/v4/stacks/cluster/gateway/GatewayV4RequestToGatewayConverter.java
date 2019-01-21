package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class GatewayV4RequestToGatewayConverter extends AbstractConversionServiceAwareConverter<GatewayV4Request, Gateway> {

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Inject
    private GatewayV4RequestValidator gatewayJsonValidator;

    @Override
    public Gateway convert(GatewayV4Request source) {
        ValidationResult validationResult = gatewayJsonValidator.validate(source);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        if (CollectionUtils.isEmpty(source.getTopologies())) {
            return null;
        }
        Gateway gateway = new Gateway();
        gatewayConvertUtil.setBasicProperties(source, gateway);
        gatewayConvertUtil.setTopologies(source, gateway);
        gatewayConvertUtil.setGatewayPathAndSsoProvider(source, gateway);
        return gateway;
    }
}
