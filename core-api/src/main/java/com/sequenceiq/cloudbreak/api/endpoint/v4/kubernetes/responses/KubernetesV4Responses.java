package com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class KubernetesV4Responses extends GeneralCollectionV4Response<KubernetesV4Response> {
    public KubernetesV4Responses(Set<KubernetesV4Response> responses) {
        super(responses);
    }

    public KubernetesV4Responses() {
        super(Sets.newHashSet());
    }
}
