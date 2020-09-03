package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckResourceRightsV4Response extends GeneralCollectionV4Response<CheckResourceRightV4SingleResponse> {

    public CheckResourceRightsV4Response(Collection<CheckResourceRightV4SingleResponse> responses) {
        super(responses);
    }

    public CheckResourceRightsV4Response() {
        super(Sets.newHashSet());
    }
}
