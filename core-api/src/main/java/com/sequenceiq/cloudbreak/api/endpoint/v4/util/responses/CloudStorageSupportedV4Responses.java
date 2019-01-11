package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudStorageSupportedV4Responses extends GeneralSetV4Response<CloudStorageSupportedV4Response> {
    public CloudStorageSupportedV4Responses(Set<CloudStorageSupportedV4Response> responses) {
        super(responses);
    }
}
