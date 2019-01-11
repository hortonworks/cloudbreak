package com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ProxyConfigModelDescription;

import io.swagger.annotations.ApiModel;

@ApiModel(description = ProxyConfigModelDescription.DESCRIPTION)
public class ProxyV4Responses extends GeneralSetV4Response<ProxyV4Response> {
    public ProxyV4Responses(Set<ProxyV4Response> responses) {
        super(responses);
    }
}
