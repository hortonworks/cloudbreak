package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE_INTERNAL;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/internal/sdx")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/internal/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxInternalEndpoint {

    @POST
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "create internal SDX cluster", produces = MediaType.APPLICATION_JSON, nickname = "createInternalSdx")
    SdxClusterResponse create(@PathParam("name") String name, @Valid SdxInternalClusterRequest createSdxClusterRequest);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RENEW_CERTIFICATE_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.RENEW_CERTIFICATE_NOTES,
            nickname = "renewInternalSdxCertificate")
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);
}
