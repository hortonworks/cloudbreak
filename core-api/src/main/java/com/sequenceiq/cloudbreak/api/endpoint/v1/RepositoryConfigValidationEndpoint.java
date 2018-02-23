package com.sequenceiq.cloudbreak.api.endpoint.v1;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RepositoryConfigsValidationOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/repositoryconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/repositoryconfigs", description = ControllerDescription.REPOSITORY_CONFIGS_VALIDATION_DESCRIPTION, protocols = "http,https")
public interface RepositoryConfigValidationEndpoint {

    @POST
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RepositoryConfigsValidationOpDescription.POST_REPOSITORY_CONFIGS_VALIDATION, produces = ContentType.JSON,
            notes = Notes.REPOSITORY_CONFIGS_VALIDATION_NOTES, nickname = "postRepositoryConfigsValidation")
    RepoConfigValidationResponse postRepositoryConfigValidationRequest(@Valid RepoConfigValidationRequest repoConfigValidationRequest);
}
