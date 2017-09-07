package com.sequenceiq.periscope.api.endpoint;

import static com.sequenceiq.periscope.doc.ApiDescription.JSON;
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

import com.sequenceiq.periscope.api.model.ScalingPolicyJson;
import com.sequenceiq.periscope.doc.ApiDescription.PolicyNotes;
import com.sequenceiq.periscope.doc.ApiDescription.PolicyOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clusters/{clusterId}/policies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/policies", description = POLICIES_DESCRIPTION, protocols = "http,https")
public interface PolicyEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_POST, produces = JSON, notes = PolicyNotes.NOTES)
    ScalingPolicyJson addScaling(@PathParam("clusterId") Long clusterId, @Valid ScalingPolicyJson json);

    @PUT
    @Path("{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_PUT, produces = JSON, notes = PolicyNotes.NOTES)
    ScalingPolicyJson setScaling(@PathParam("clusterId") Long clusterId, @PathParam("policyId") Long policyId,
            @Valid ScalingPolicyJson scalingPolicy);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_GET, produces = JSON, notes = PolicyNotes.NOTES)
    List<ScalingPolicyJson> getScaling(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PolicyOpDescription.POLICY_DELETE, produces = JSON, notes = PolicyNotes.NOTES)
    void deletePolicy(@PathParam("clusterId") Long clusterId, @PathParam("policyId") Long policyId);

}
