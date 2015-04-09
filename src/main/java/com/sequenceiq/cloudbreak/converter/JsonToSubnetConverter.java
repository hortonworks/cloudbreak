package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.domain.SubnetJson;

@Component
public class JsonToSubnetConverter extends AbstractConversionServiceAwareConverter<SubnetJson, Subnet> {
    @Override
    public Subnet convert(SubnetJson json) {
        return new Subnet(json.getSubnet());
    }
}
