package com.sequenceiq.authorization.info.model;

import java.util.Collection;

import com.google.common.collect.Sets;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CheckRightV4Response extends GeneralCollectionV4Response<CheckRightV4SingleResponse> {

    public CheckRightV4Response(Collection<CheckRightV4SingleResponse> responses) {
        super(responses);
    }

    public CheckRightV4Response() {
        super(Sets.newHashSet());
    }
}
