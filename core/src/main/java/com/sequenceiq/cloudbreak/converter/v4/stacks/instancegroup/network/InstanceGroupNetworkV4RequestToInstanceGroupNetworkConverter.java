package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@Component
public class InstanceGroupNetworkV4RequestToInstanceGroupNetworkConverter
    extends AbstractConversionServiceAwareConverter<InstanceGroupNetworkV4Request, InstanceGroupNetwork> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupNetworkV4RequestToInstanceGroupNetworkConverter.class);

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceGroupNetwork convert(InstanceGroupNetworkV4Request source) {
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        CloudPlatform cloudPlatform = providerParameterCalculator.get(source).getCloudPlatform();
        network.setCloudPlatform(cloudPlatform.name());
        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        Optional.ofNullable(parameters).map(toJson()).ifPresent(network::setAttributes);
        return network;
    }

    private Function<Map<String, Object>, Json> toJson() {
        return value -> {
            try {
                return new Json(value);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Failed to parse instancegroup network parameters as JSON.", e);
                throw new BadRequestException("Invalid instancegroup network parameter format, valid JSON expected.");
            }
        };
    }

}
