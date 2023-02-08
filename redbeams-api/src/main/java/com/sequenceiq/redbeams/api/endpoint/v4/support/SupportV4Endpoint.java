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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/v4/support")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/support", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SupportV4Endpoint {
    @POST
    @Path("certificate")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseServerOpDescription.CERT_SWAP, notes = Notes.DatabaseServerNotes.CERT_SWAP,
            consumes = MediaType.APPLICATION_JSON, nickname = "changeMockCertificate")
    CertificateSwapV4Response swapCertificate(
            @Valid @ApiParam(ModelDescriptions.SUPPORT_CERTIFICATE_REQUEST) CertificateSwapV4Request request
    );
}
