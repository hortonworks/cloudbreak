package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class WorkspaceV4Responses extends GeneralSetV4Response<WorkspaceV4Response> {

    public WorkspaceV4Responses(Set<WorkspaceV4Response> responses) {
        super(responses);
    }
}
