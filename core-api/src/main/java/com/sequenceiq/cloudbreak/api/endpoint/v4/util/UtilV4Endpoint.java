package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.ClientVersionV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.StackVersionV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VersionCheckV4Result;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/utils", description = ControllerDescription.UTIL_V4_DESCRIPTION, protocols = "http,https")
public interface UtilV4Endpoint {

    @GET
    @Path("client")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CHECK_CLIENT_VERSION, produces = ContentType.JSON,
            nickname = "checkClientVersionV4")
    VersionCheckV4Result checkClientVersion(@BeanParam ClientVersionV4Filter clientVersionV4Filter);

    @GET
    @Path("stack_matrix")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.STACK_MATRIX, produces = ContentType.JSON, nickname = "getStackMatrixUtilV4")
    StackMatrixV4 getStackMatrix();

    @GET
    @Path("cloud_storage_matrix")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CLOUD_STORAGE_MATRIX, produces = ContentType.JSON, nickname = "getCloudStorageMatrixV4",
            notes = "Define stack version at least at patch level eg. 2.6.0")
    CloudStorageSupportedV4Responses getCloudStorageMatrix(@BeanParam StackVersionV4Filter stackVersionV4Filter);

    @POST
    @Path("validate_repository")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RepositoryConfigsValidationOpDescription.POST_REPOSITORY_CONFIGS_VALIDATION, produces = ContentType.JSON,
            notes = Notes.REPOSITORY_CONFIGS_VALIDATION_NOTES, nickname = "repositoryConfigsValidationV4")
    RepoConfigValidationV4Response repositoryConfigValidationRequest(@Valid RepoConfigValidationV4Request repoConfigValidationV4Request);
}
