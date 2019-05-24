package com.sequenceiq.environment.api.v1.proxy.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;
import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ProxyConfigDescription.DESCRIPTION)
public class ProxyResponses extends GeneralCollectionV1Response<ProxyResponse> {
    public ProxyResponses(Set<ProxyResponse> responses) {
        super(responses);
    }

    public ProxyResponses() {
        super(Sets.newHashSet());
    }
}
