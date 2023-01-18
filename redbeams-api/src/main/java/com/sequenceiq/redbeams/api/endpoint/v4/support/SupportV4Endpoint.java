package com.sequenceiq.redbeams.api.endpoint.v4.support;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
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
}
