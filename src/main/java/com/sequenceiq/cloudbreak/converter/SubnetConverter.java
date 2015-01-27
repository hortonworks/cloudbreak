package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.domain.SubnetJson;

@Component
public class SubnetConverter extends AbstractConverter<SubnetJson, Subnet> {

    @Override
    public SubnetJson convert(Subnet entity) {
        return new SubnetJson(entity.getCidr());
    }

    @Override
    public Subnet convert(SubnetJson json) {
        return new Subnet(json.getSubnet());
    }

    public Set<Subnet> convertAllJsonToEntity(Collection<SubnetJson> jsonList, Stack stack) {
        Set<Subnet> subnetSet = convertAllJsonToEntity(jsonList);
        for (Subnet subnet : subnetSet) {
            subnet.setStack(stack);
        }
        return subnetSet;
    }
}
