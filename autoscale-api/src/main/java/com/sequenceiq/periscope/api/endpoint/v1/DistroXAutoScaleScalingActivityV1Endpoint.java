package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.SCALING_ACTIVITY_DESCRIPTION;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/v1/scaling_activities")
@Api(value = "/v1/scaling_activities", description = SCALING_ACTIVITY_DESCRIPTION, protocols = "http,https")
public interface DistroXAutoScaleScalingActivityV1Endpoint {

    /**
     * Returns Scaling activities in a particular duration.
     * @param clusterName Name of cluster for which we require the scaling activities.
     * @param durationInMinutes the duration in which we want to get all the scaling activities.
     * @return List of scaling Activities in the duration.
     */
    @GET
    @Path("cluster_name/{clusterName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesInDurationByClusterName(
            @PathParam("clusterName") @NotNull String clusterName,
            @QueryParam("durationInMinutes") @DefaultValue("60") long durationInMinutes,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns Scaling activities in a particular duration.
     * @param clusterCrn Crn of cluster for which we require the scaling activities.
     * @param durationInMinutes the duration in which we want to get all the scaling activities.
     * @return List of scaling activities after the input time stamp value.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesInDurationByClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @QueryParam("durationInMinutes") @DefaultValue("60") long durationInMinutes,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns Scaling activity for a particular operation ID value.
     * @param clusterCrn Crn of cluster where the scaling activity belongs.
     * @param operationId the value for which we want the scaling activity value.
     * @return A particular scaling activity value associated with the input Id value.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}/operation_id/{operationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_OPERATION_ID,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    DistroXAutoscaleScalingActivityResponse getScalingActivityUsingOperationId(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @PathParam("operationId") @NotNull String operationId);

    /**
     * Returns All the failed scaling activities in the given duration.
     * @param clusterName Name of cluster for which we require all the failed scaling activities.
     * @param durationInMinutes the duration in minutes in which we want to get all the failed scaling activities.
     * @return All the scaling activities which have failed in the given duration.
     */
    @GET
    @Path("cluster_name/{clusterName}/failed_activities")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.FAILED_SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesInGivenDurationByClusterName(
            @PathParam("clusterName") @NotNull String clusterName,
            @QueryParam("durationInMinutes") @DefaultValue("60") long durationInMinutes,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns All the failed scaling activities in the given duration.
     * @param clusterCrn Crn of cluster for which we require all the failed scaling activities.
     * @param durationInMinutes the duration in minutes in which we want to get all the failed scaling activities.
     * @return All the scaling activities which have failed in the given duration.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}/failed_activities")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.FAILED_SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesInGivenDurationByClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @QueryParam("durationInMinutes") @DefaultValue("60") long durationInMinutes,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns all the scaling activities having start_time in the given time interval between startTimeFrom and startTimeUntil.
     * @param clusterName Name of cluster for which we require the scaling activities.
     * @param startTimeFromInEpochMilliSec time in epoch milliseconds after which we want the activities.
     * @param startTimeUntilInEpochMilliSec time in epoch milliseconds till which we want the activities.
     * @return All the scaling activities between the start and end time.
     */
    @GET
    @Path("cluster_name/{clusterName}/time_interval")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_BETWEEN_TIME_INTERVAL,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesBetweenIntervalByClusterName(
            @PathParam("clusterName") @NotNull String clusterName,
            @QueryParam("startTimeFromInEpochMilliSec")  long startTimeFromInEpochMilliSec,
            @QueryParam("startTimeUntilInEpochMilliSec") long startTimeUntilInEpochMilliSec,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns all the scaling activities having start_time in the given time interval between start and end time.
     * @param clusterCrn Crn of cluster for which we require the scaling activities.
     * @param startTimeFromInEpochMilliSec time in epoch milliseconds after which we want the activities.
     * @param startTimeUntilInEpochMilliSec time in epoch milliseconds till which we want the activities.
     * @return All the scaling activities between the start and end time.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}/time_interval")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_BETWEEN_TIME_INTERVAL,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesBetweenIntervalByClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @QueryParam("startTimeFromInEpochMilliSec") long startTimeFromInEpochMilliSec,
            @QueryParam("startTimeUntilInEpochMilliSec") long startTimeUntilInEpochMilliSec,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

}