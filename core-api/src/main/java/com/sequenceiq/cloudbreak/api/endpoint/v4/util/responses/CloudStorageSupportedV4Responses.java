package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema
public class CloudStorageSupportedV4Responses extends GeneralCollectionV4Response<CloudStorageSupportedV4Response> {
    public CloudStorageSupportedV4Responses(Set<CloudStorageSupportedV4Response> responses) {
        super(responses);
    }

    public CloudStorageSupportedV4Responses() {
        super(Sets.newHashSet());
    }
}
