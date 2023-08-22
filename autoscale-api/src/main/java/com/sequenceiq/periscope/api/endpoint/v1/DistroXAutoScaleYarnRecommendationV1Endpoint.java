package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.YARN_RECOMMENDATION_DESCRIPTION;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.periscope.api.model.DistroXAutoScaleYarnRecommendationResponse;
import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/v1/yarn_recommendation")
@Api(value = "/v1/yarn_recommendation", description = YARN_RECOMMENDATION_DESCRIPTION, protocols = "http,https")
public interface DistroXAutoScaleYarnRecommendationV1Endpoint {

    @GET
    @Path("cluster_crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.YARN_RECOMMENDATION,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    DistroXAutoScaleYarnRecommendationResponse getYarnRecommendation(
            @PathParam("clusterCrn") @NotNull String clusterCrn) throws Exception;
}
