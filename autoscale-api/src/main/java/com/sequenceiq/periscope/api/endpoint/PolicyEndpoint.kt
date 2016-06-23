package com.sequenceiq.periscope.api.endpoint

import com.sequenceiq.periscope.doc.ApiDescription.JSON
import com.sequenceiq.periscope.doc.ApiDescription.POLICIES_DESCRIPTION

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.periscope.doc.ApiDescription
import com.sequenceiq.periscope.api.model.ScalingPolicyJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/clusters/{clusterId}/policies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/policies", description = POLICIES_DESCRIPTION, position = 4)
interface PolicyEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.PolicyOpDescription.POLICY_POST, produces = JSON, notes = ApiDescription.PolicyNotes.NOTES)
    fun addScaling(@PathParam(value = "clusterId") clusterId: Long?, @Valid json: ScalingPolicyJson): ScalingPolicyJson

    @PUT
    @Path("{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.PolicyOpDescription.POLICY_PUT, produces = JSON, notes = ApiDescription.PolicyNotes.NOTES)
    fun setScaling(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "policyId") policyId: Long?,
                   @Valid scalingPolicy: ScalingPolicyJson): ScalingPolicyJson

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.PolicyOpDescription.POLICY_GET, produces = JSON, notes = ApiDescription.PolicyNotes.NOTES)
    fun getScaling(@PathParam(value = "clusterId") clusterId: Long?): List<ScalingPolicyJson>

    @DELETE
    @Path("{policyId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.PolicyOpDescription.POLICY_DELETE, produces = JSON, notes = ApiDescription.PolicyNotes.NOTES)
    fun deletePolicy(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "policyId") policyId: Long?)

}
