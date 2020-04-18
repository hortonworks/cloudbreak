package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.cloudera.cdp.shaded.javax.ws.rs.core.MediaType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AuthorizeForAutoscaleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/autoscale")
@RetryAndMetrics
@Consumes(APPLICATION_JSON)
@Api(value = "/autoscale", description = ControllerDescription.AUTOSCALE_DESCRIPTION, protocols = "http,https",
        consumes = APPLICATION_JSON)
public interface AutoscaleV4Endpoint {

    @PUT
    @Path("/stack/crn/{crn}/{userId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "putStackForAutoscale")
    void putStack(@PathParam("crn") String crn, @PathParam("userId") String userId, @Valid UpdateStackV4Request updateRequest);

    @PUT
    @Path("/stack/crn/{crn}/{userId}/cluster")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "putClusterForAutoscale")
    void putCluster(@PathParam("crn") String crn, @PathParam("userId") String userId, @Valid UpdateClusterV4Request updateRequest);

    @POST
    @Path("ambari")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getStackForAmbariForAutoscale")
    StackV4Response getStackForAmbari(@Valid AmbariAddressV4Request json);

    @GET
    @Path("stack/all")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_ALL, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getAllStackForAutoscale")
    AutoscaleStackV4Responses getAllForAutoscale();

    @GET
    @Path("/stack/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_CRN, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getStackForAutoscale")
    StackV4Response get(@PathParam("crn") String crn);

    @GET
    @Path("/stack/crn/{crn}/status")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_CRN, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getStackStatusForAutoscale")
    StackStatusV4Response getStatusByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/stack/crn/{crn}/authorize/{userId}/{tenant}/{permission}")
    @Produces(APPLICATION_JSON)
    AuthorizeForAutoscaleV4Response authorizeForAutoscale(@PathParam("crn") String crn, @PathParam("userId") String userId, @PathParam("tenant") String tenant,
            @PathParam("permission") String permission);

    @GET
    @Path("/stack/crn/{crn}/certificate")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getCertificateStackForAutoscale")
    CertificateV4Response getCertificate(@PathParam("crn") String crn);

    @DELETE
    @Path("/stack/crn/{crn}/instances")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "decommissionInstancesForClusterCrn")
    void decommissionInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @QueryParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("instanceId") @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @GET
    @Path("clusterproxy")
    @Produces(MediaType.APPLICATION_JSON)
    ClusterProxyConfiguration getClusterProxyconfiguration();

    @GET
    @Path("distroXInstanceTypes/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    Set<String> getSupportedDistroXInstanceTypes(@PathParam("cloudPlatform") String cloudPlatform);
}
