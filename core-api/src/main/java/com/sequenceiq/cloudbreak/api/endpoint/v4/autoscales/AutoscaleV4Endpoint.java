package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AuthorizeForAutoscaleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/autoscale")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/autoscale", description = ControllerDescription.AUTOSCALE_DESCRIPTION, protocols = "http,https")
public interface AutoscaleV4Endpoint {

    @PUT
    @Path("/stack/{id}/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "putStackForAutoscale")
    void putStack(@PathParam("id") Long id, @PathParam("userId") String userId, @Valid UpdateStackV4Request updateRequest);

    @PUT
    @Path("/stack/{id}/{userId}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "putClusterForAutoscale")
    void putCluster(@PathParam("id") Long id, @PathParam("userId") String userId, @Valid UpdateClusterV4Request updateRequest);

    @POST
    @Path("ambari")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "getStackForAmbariForAutoscale")
    StackV4Response getStackForAmbari(@Valid AmbariAddressV4Request json);

    @GET
    @Path("stack/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_ALL, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getAllStackForAutoscale")
    AutoscaleStackV4Responses getAllForAutoscale();

    @POST
    @Path("/stack/{id}/cluster/failurereport")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.FAILURE_REPORT, produces = ContentType.JSON, notes = Notes.FAILURE_REPORT_NOTES,
            nickname = "failureReportClusterForAutoscale")
    void failureReport(@PathParam("id") Long stackId, FailureReportV4Request failureReport);

    @GET
    @Path("/stack/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getStackForAutoscale")
    StackV4Response get(@PathParam("id") Long id);

    @GET
    @Path("/stack/{id}/authorize/{userId}/{tenant}/{permission}")
    @Produces(MediaType.APPLICATION_JSON)
    AuthorizeForAutoscaleV4Response authorizeForAutoscale(@PathParam("id") Long id, @PathParam("userId") String userId, @PathParam("tenant") String tenant,
            @PathParam("permission") String permission);

    @GET
    @Path("/stack/{id}/certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "getCertificateStackForAutoscale")
    CertificateV4Response getCertificate(@PathParam("id") Long stackId);
}
