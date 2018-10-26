package com.sequenceiq.cloudbreak.api.endpoint.autoscale;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
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
public interface AutoscaleEndpoint {

    @PUT
    @Path("/stack/{id}/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "putStackForAutoscale")
    Response putStack(@PathParam("id") Long id, @PathParam("userId") String userId, @Valid UpdateStackJson updateRequest);

    @PUT
    @Path("/stack/{id}/{userId}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "putClusterForAutoscale")
    Response putCluster(@PathParam("id") Long id, @PathParam("userId") String userId, @Valid UpdateClusterJson updateRequest);

    @POST
    @Path("ambari")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "getStackForAmbariForAutoscale")
    StackResponse getStackForAmbari(@Valid AmbariAddressJson json);

    @GET
    @Path("stack/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_ALL, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getAllStackForAutoscale")
    Set<AutoscaleStackResponse> getAllForAutoscale();

    @POST
    @Path("/stack/{id}/cluster/failurereport")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.FAILURE_REPORT, produces = ContentType.JSON, notes = Notes.FAILURE_REPORT_NOTES,
            nickname = "failureReportClusterForAutoscale")
    Response failureReport(@PathParam("id") Long stackId, FailureReport failureReport);

    @GET
    @Path("/stack/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getStackForAutoscale")
    StackResponse get(@PathParam("id") Long id);

    @GET
    @Path("/stack/{id}/cluster/full")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getFullClusterForAutoscale")
    AutoscaleClusterResponse getForAutoscale(@PathParam("id") Long id);

    @GET
    @Path("/stack/{id}/authorize/{userId}/{tenant}/{permission}")
    @Produces(MediaType.APPLICATION_JSON)
    Boolean authorizeForAutoscale(@PathParam("id") Long id, @PathParam("userId") String userId, @PathParam("tenant") String tenant,
            @PathParam("permission") String permission);

    @GET
    @Path("/stack/{id}/certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "getCertificateStackForAutoscale")
    CertificateResponse getCertificate(@PathParam("id") Long stackId);
}
