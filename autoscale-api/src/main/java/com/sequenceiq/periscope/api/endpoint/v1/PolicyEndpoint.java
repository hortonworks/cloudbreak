package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.POLICIES_DESCRIPTION;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyResponse;
import com.sequenceiq.periscope.doc.ApiDescription.PolicyNotes;
import com.sequenceiq.periscope.doc.ApiDescription.PolicyOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/clusters/{clusterId}/policies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(hidden = true, value = "/v1/policies", description = POLICIES_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface PolicyEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_POST, produces = MediaType.APPLICATION_JSON, notes = PolicyNotes.NOTES)
    ScalingPolicyResponse addScalingPolicy(@PathParam("clusterId") Long clusterId, @Valid ScalingPolicyRequest json);

    @PUT
    @Path("{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_PUT, produces = MediaType.APPLICATION_JSON, notes = PolicyNotes.NOTES)
    ScalingPolicyResponse updateScalingPolicy(@PathParam("clusterId") Long clusterId, @PathParam("policyId") Long policyId,
            @Valid ScalingPolicyRequest scalingPolicy);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_GET, produces = MediaType.APPLICATION_JSON, notes = PolicyNotes.NOTES)
    List<ScalingPolicyResponse> getScalingPolicies(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_DELETE, produces = MediaType.APPLICATION_JSON, notes = PolicyNotes.NOTES)
    void deleteScalingPolicy(@PathParam("clusterId") Long clusterId, @PathParam("policyId") Long policyId);

}
