package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.SubnetJson;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Subnet;

@Component
public class JsonToSubnetConverter extends AbstractConversionServiceAwareConverter<SubnetJson, Subnet> {
    @Override
    public Subnet convert(SubnetJson json) {
        return new Subnet(json.getSubnet());
    }
}
