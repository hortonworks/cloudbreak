package com.sequenceiq.environment.api.proxy.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;
import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
public class ProxyV1Responses extends GeneralCollectionV1Response<ProxyV1Response> {
    public ProxyV1Responses(Set<ProxyV1Response> responses) {
        super(responses);
    }

    public ProxyV1Responses() {
        super(Sets.newHashSet());
    }
}
