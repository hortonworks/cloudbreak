package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Collection;
import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueriesResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.filesystem.CloudStorageSupportedResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsBuildResult;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/util")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/util", description = ControllerDescription.UTIL_DESCRIPTION, protocols = "http,https")
public interface UtilEndpoint {

    @GET
    @Path("client/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CHECK_CLIENT_VERSION, produces = ContentType.JSON,
            nickname = "checkClientVersion")
    VersionCheckResult checkClientVersion(@PathParam("version") String version);

    @POST
    @Path("ambari-database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.TEST_DATABASE, produces = ContentType.JSON, nickname = "testAmbariDatabaseUtil")
    AmbariDatabaseTestResult testAmbariDatabase(@Valid AmbariDatabaseDetailsJson ambariDatabaseDetailsJson);

    @POST
    @Path("rds-database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CREATE_DATABASE, produces = ContentType.JSON, nickname = "createRDSDatabaseUtil")
    RdsBuildResult buildRdsConnection(@Valid RDSBuildRequest rdsBuildRequest, @QueryParam("target") Set<String> targets);

    @GET
    @Path("stackmatrix")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.STACK_MATRIX, produces = ContentType.JSON, nickname = "getStackMatrixUtil")
    StackMatrix getStackMatrix();

    @GET
    @Path("knoxservices/{blueprintName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.KNOX_SERVICES, produces = ContentType.JSON, nickname = "getKnoxServices")
    Collection<ExposedServiceResponse> getKnoxServices(@PathParam("blueprintName") String blueprintId);

    @GET
    @Path("cloudstoragematrix")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CLOUD_STORAGE_MATRIX, produces = ContentType.JSON, nickname = "getCloudStorageMatrix",
            notes = "Define stack version at least at patch level eg. 2.6.0")
    Collection<CloudStorageSupportedResponse> getCloudStorageMatrix(@QueryParam("stackVersion") String stackVersion);

    @POST
    @Path("custom-parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CUSTOM_PARAMETERS, produces = ContentType.JSON, nickname = "getCustomParameters")
    ParametersQueryResponse getCustomParameters(ParametersQueryRequest parametersQueryRequest);

    @POST
    @Path("filesystem-parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.FILE_SYSTEM_PARAMETERS, produces = ContentType.JSON, nickname = "getFileSystemParameters")
    StructuredParameterQueriesResponse getFileSystemParameters(StructuredParametersQueryRequest structuredParametersQueryRequest);
}
