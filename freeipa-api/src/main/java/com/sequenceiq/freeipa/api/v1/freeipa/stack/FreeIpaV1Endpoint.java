package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa", protocols = "http,https")
public interface FreeIpaV1Endpoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CREATE, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "createFreeIpaV1")
    DescribeFreeIpaResponse create(@Valid CreateFreeIpaRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.GET_BY_ENVID, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "getFreeIpaByEnvironmentV1")
    DescribeFreeIpaResponse describe(@QueryParam("environment") String environmentCrn);

    @GET
    @Path("ca.crt")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = FreeIpaOperationDescriptions.GET_ROOTCERTIFICATE_BY_ENVID, produces = ContentType.TEXT_PLAIN, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "getFreeIpaRootCertificateByEnvironmentV1")
    String getRootCertificate(@QueryParam("environment") String environmentCrn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.DELETE_BY_ENVID, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "deleteFreeIpaByEnvironmentV1")
    void delete(@QueryParam("environment") String environmentCrn);
}
