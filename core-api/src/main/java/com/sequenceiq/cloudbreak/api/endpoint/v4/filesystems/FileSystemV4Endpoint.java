package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.FileSystemOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/file_systems")
@Tag(name = "/v4/{workspaceId}/file_systems", description = ControllerDescription.FILESYSTEMS_V4_DESCRIPTION)
public interface FileSystemV4Endpoint {

    @GET
    @Path("parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FileSystemOpDescription.FILE_SYSTEM_PARAMETERS, operationId = "getFileSystemParameters",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FileSystemParameterV4Responses getFileSystemParameters(
            @PathParam("workspaceId") Long workspaceId,
            @NotNull @QueryParam("blueprintName") String blueprintName,
            @NotNull @QueryParam("clusterName") String clusterName,
            @QueryParam("accountName") String accountName,
            @NotNull @QueryParam("storageName") String storageName,
            @NotNull @QueryParam("fileSystemType") String fileSystemType,
            @QueryParam("attachedCluster") @DefaultValue("false") Boolean attachedCluster,
            @QueryParam("secure") @DefaultValue("false") Boolean secure);

    @GET
    @Path("parameters_internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FileSystemOpDescription.FILE_SYSTEM_PARAMETERS_INTERNAL,
            operationId = "getFileSystemParametersInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FileSystemParameterV4Responses getFileSystemParametersInternal(
            @PathParam("workspaceId") Long workspaceId,
            @NotNull @QueryParam("blueprintName") String blueprintName,
            @NotNull @QueryParam("clusterName") String clusterName,
            @QueryParam("accountName") String accountName,
            @NotNull @QueryParam("storageName") String storageName,
            @NotNull @QueryParam("fileSystemType") String fileSystemType,
            @QueryParam("attachedCluster") @DefaultValue("false") Boolean attachedCluster,
            @QueryParam("secure") @DefaultValue("false") Boolean secure,
            @QueryParam("accountId") String accountId);
}
