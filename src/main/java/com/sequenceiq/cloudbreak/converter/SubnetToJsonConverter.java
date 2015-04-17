package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.SubnetJson;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Subnet;

@Component
public class SubnetToJsonConverter extends AbstractConversionServiceAwareConverter<Subnet, SubnetJson> {

    @Override
    public SubnetJson convert(Subnet entity) {
        return new SubnetJson(entity.getCidr());
    }
}
