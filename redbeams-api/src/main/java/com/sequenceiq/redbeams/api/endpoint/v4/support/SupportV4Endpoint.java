package com.sequenceiq.redbeams.api.endpoint.v4.support;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.doc.Notes;
import com.sequenceiq.redbeams.doc.OperationDescriptions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/support")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/support")
public interface SupportV4Endpoint {
    @POST
    @Path("certificate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.DatabaseServerOpDescription.CERT_SWAP, description = Notes.DatabaseServerNotes.CERT_SWAP,
            operationId = "changeMockCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CertificateSwapV4Response swapCertificate(
            @Valid @Parameter(description = ModelDescriptions.SUPPORT_CERTIFICATE_REQUEST) CertificateSwapV4Request request
    );

    @GET
    @Path("internal/get_latest_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = OperationDescriptions.DatabaseServerOpDescription.LATEST_CERTIFICATE_LIST,
            description = Notes.DatabaseServerNotes.LATEST_CERT, operationId = "getLatestDatabaseCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SslCertificateEntryResponse getLatestCertificate(
            @QueryParam("cloudPlatform") String cloudPlatform,
            @QueryParam("region") String region);

    @GET
    @Path("/internal/defaults/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get instances by platform",
            operationId = "getPlatformSupportRequirementsByPlatform",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RedBeamsPlatformSupportRequirements getInstanceTypesByPlatform(@PathParam("cloudPlatform") String cloudPlatform);

}
