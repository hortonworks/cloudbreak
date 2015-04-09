package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.domain.SubnetJson;

@Component
public class SubnetToJsonConverter extends AbstractConversionServiceAwareConverter<Subnet, SubnetJson> {

    @Override
    public SubnetJson convert(Subnet entity) {
        return new SubnetJson(entity.getCidr());
    }
}
