package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.CloudbreakPolicy;
import com.sequenceiq.periscope.rest.json.CloudbreakPolicyJson;

@Component
public class CloudbreakPolicyConverter extends AbstractConverter<CloudbreakPolicyJson, CloudbreakPolicy> {

    @Override
    public CloudbreakPolicy convert(CloudbreakPolicyJson source) {
        return new CloudbreakPolicy(source);
    }

}
