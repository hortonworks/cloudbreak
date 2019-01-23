package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class WorkspaceV4Responses extends GeneralCollectionV4Response<WorkspaceV4Response> {

    public WorkspaceV4Responses(Set<WorkspaceV4Response> responses) {
        super(responses);
    }

    public WorkspaceV4Responses() {
        super(Sets.newHashSet());
    }
}
