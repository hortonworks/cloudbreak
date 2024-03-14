package com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint;


import static com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint.PrivateControlPlaneOpDescription.CONTROL_PLANE_NOTES;
import static com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint.PrivateControlPlaneOpDescription.DEREGISTER;
import static com.sequenceiq.remoteenvironment.api.v1.controlplane.endpoint.PrivateControlPlaneOpDescription.REGISTER;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneDeRegistrationResponse;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.PrivateControlPlaneRegistrationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/control_plane")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/control_plane", description = "Private Control Plane operations")
public interface PrivateControlPlaneEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REGISTER,
            description = CONTROL_PLANE_NOTES,
            operationId = "registerPrivateControlPlaneV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PrivateControlPlaneRegistrationResponse register(@Valid PrivateControlPlaneRegistrationRequest request);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DEREGISTER,
            description = CONTROL_PLANE_NOTES,
            operationId = "deregisterPrivateControlPlaneV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PrivateControlPlaneDeRegistrationResponse deregister(@PathParam("crn") String crn);

}