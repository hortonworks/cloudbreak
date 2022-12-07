package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE_INTERNAL;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
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

    @PUT
    @Path("crn/{crn}/updateDbEngineVersion")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "update the db engine version in the service db", produces = MediaType.APPLICATION_JSON, nickname = "updateDbEngineVersion")
    void updateDbEngineVersion(@PathParam("crn") String crn,
            @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version") @QueryParam("dbEngineVersion") String dbEngineVersion);

    @PUT
    @Path("crn/{crn}/modify_proxy")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Initiates the modification of the proxy config", produces = MediaType.APPLICATION_JSON, nickname = "modifyInternalSdxProxyConfig")
    FlowIdentifier modifyProxy(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) @PathParam("crn") String crn,
            @ValidCrn(resource = CrnResourceDescriptor.PROXY) @QueryParam("previousProxy") String previousProxyCrn,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);
}
