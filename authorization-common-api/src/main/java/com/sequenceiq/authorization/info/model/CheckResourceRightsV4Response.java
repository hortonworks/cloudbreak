package com.sequenceiq.authorization.info.model;

import java.util.Collection;

import com.google.common.collect.Sets;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CheckResourceRightsV4Response extends GeneralCollectionV4Response<CheckResourceRightV4SingleResponse> {

    public CheckResourceRightsV4Response(Collection<CheckResourceRightV4SingleResponse> responses) {
        super(responses);
    }

    public CheckResourceRightsV4Response() {
        super(Sets.newHashSet());
    }
}
