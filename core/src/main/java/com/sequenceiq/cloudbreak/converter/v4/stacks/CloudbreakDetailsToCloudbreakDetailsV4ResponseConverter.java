package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;

@Component
public class CloudbreakDetailsToCloudbreakDetailsV4ResponseConverter {

    public CloudbreakDetailsV4Response convert(CloudbreakDetails source) {
        CloudbreakDetailsV4Response json = new CloudbreakDetailsV4Response();
        json.setVersion(source.getVersion());
        return json;
    }
}
