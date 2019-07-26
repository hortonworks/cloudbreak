package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckRightV4Response extends GeneralCollectionV4Response<CheckRightV4SingleResponse> {

    public CheckRightV4Response(Collection<CheckRightV4SingleResponse> responses) {
        super(responses);
    }

    public CheckRightV4Response() {
        super(Sets.newHashSet());
    }
}
