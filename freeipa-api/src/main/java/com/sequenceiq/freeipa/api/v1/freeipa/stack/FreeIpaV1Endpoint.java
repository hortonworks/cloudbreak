package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
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
    DescribeFreeIpaResponse describe(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.LIST_BY_ACCOUNT, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "listFreeIpaClustersByAccountV1")
    List<ListFreeIpaResponse> list();

    @GET
    @Path("ca.crt")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = FreeIpaOperationDescriptions.GET_ROOTCERTIFICATE_BY_ENVID, produces = ContentType.TEXT_PLAIN, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "getFreeIpaRootCertificateByEnvironmentV1")
    String getRootCertificate(@QueryParam("environment") @NotEmpty String environmentCrn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.DELETE_BY_ENVID, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "deleteFreeIpaByEnvironmentV1")
    void delete(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CLEANUP, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "cleanupV1")
    CleanupResponse cleanup(@Valid CleanupRequest request) throws Exception;

    @PUT
    @Path("start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.START, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "startV1")
    void start(@QueryParam("environment") @NotEmpty String environmentCrn) throws Exception;

    @PUT
    @Path("stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.STOP, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "stopV1")
    void stop(@QueryParam("environment") @NotEmpty String environmentCrn) throws Exception;

    @POST
    @Path("cluster-proxy/register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REGISTER_WITH_CLUSTER_PROXY, produces = ContentType.JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "clusterProxyRegisterV1")
    String registerWithClusterProxy(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("cluster-proxy/deregister")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.DEREGISTER_WITH_CLUSTER_PROXY, produces = ContentType.JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "clusterProxyDeregisterV1")
    void deregisterWithClusterProxy(@QueryParam("environment") @NotEmpty String environmentCrn);
}
