package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel
public class CloudStorageSupportedV4Responses extends GeneralSetV4Response<CloudStorageSupportedV4Response> {
    public CloudStorageSupportedV4Responses(Set<CloudStorageSupportedV4Response> responses) {
        super(responses);
    }

    public CloudStorageSupportedV4Responses() {
        super(Sets.newHashSet());
    }
}
