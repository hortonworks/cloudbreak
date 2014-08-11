package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.policies.cloudbreak.CloudbreakPolicy;
import com.sequenceiq.periscope.rest.json.CloudbreakPolicyJson;

@Component
public class CloudbreakPolicyConverter extends AbstractConverter<CloudbreakPolicyJson, CloudbreakPolicy> {

    @Override
    public CloudbreakPolicy convert(CloudbreakPolicyJson source) {
        return new CloudbreakPolicy(source.getScaleUpRules(), source.getScaleDownRules(), source.getJarUrl());
    }

    @Override
    public CloudbreakPolicyJson convert(CloudbreakPolicy source) {
        return new CloudbreakPolicyJson(source.getScaleUpConfig(), source.getScaleDownConfig(), source.getJarUrl());
    }
}
