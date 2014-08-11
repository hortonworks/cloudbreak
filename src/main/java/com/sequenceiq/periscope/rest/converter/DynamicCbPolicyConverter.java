package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.policies.cloudbreak.CloudbreakPolicy;
import com.sequenceiq.periscope.rest.json.DynamicCbPolicyJson;

@Component
public class DynamicCbPolicyConverter extends AbstractConverter<DynamicCbPolicyJson, CloudbreakPolicy> {

    @Override
    public CloudbreakPolicy convert(DynamicCbPolicyJson source) {
        return new CloudbreakPolicy(source.getScaleUpRules(), source.getScaleDownRules(), source.getJarUrl());
    }

}
