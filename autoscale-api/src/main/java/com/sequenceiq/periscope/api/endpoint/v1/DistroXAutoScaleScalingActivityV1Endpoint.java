package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.SCALING_ACTIVITY_DESCRIPTION;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/v1/scaling_activities")
@Tag(name = "/v1/scaling_activities", description = SCALING_ACTIVITY_DESCRIPTION)
public interface DistroXAutoScaleScalingActivityV1Endpoint {

    /**
     * Returns Scaling activities in a particular duration.
     * @param clusterName Name of cluster for which we require the scaling activities.
     * @param durationInMinutes the duration in which we want to get all the scaling activities.
     * @return Page of scaling Activities in the duration.
     */
    @GET
    @Path("cluster_name/{clusterName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getScalingActivitiesInGivenDurationByClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesInGivenDurationByClusterName(
            @PathParam("clusterName") @NotNull String clusterName,
            @QueryParam("durationInMinutes") @DefaultValue("60") long durationInMinutes,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns Scaling activities in a particular duration.
     * @param clusterCrn Crn of cluster for which we require the scaling activities.
     * @param durationInMinutes the duration in which we want to get all the scaling activities.
     * @return Page of scaling activities after the input time stamp value.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getScalingActivitiesInGivenDurationByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesInGivenDurationByClusterCrn(
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
    @Operation(summary = ApiDescription.ClusterOpDescription.SCALING_OPERATION_ID,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getScalingActivityUsingOperationIdAndClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleScalingActivityResponse getScalingActivityUsingOperationIdAndClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @PathParam("operationId") @NotNull String operationId);

    /**
     * Returns Scaling activity for a particular operation ID value.
     * @param clusterName Name of cluster where the scaling activity belongs.
     * @param operationId the value for which we want the scaling activity value.
     * @return A particular scaling activity value associated with the input Id value and cluster name.
     */
    @GET
    @Path("cluster_name/{clusterName}/operation_id/{operationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ApiDescription.ClusterOpDescription.SCALING_OPERATION_ID, description = ApiDescription.DistroXClusterNotes.NOTES,
        operationId = "getScalingActivityUsingOperationIdAndClusterName",
        responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXAutoscaleScalingActivityResponse getScalingActivityUsingOperationIdAndClusterName(
            @PathParam("clusterName") @NotNull String clusterName,
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
    @Operation(summary = ApiDescription.ClusterOpDescription.FAILED_SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getFailedScalingActivitiesInGivenDurationByClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesInGivenDurationByClusterName(
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
    @Operation(summary = ApiDescription.ClusterOpDescription.FAILED_SCALING_ACTIVITIES_IN_DURATION_IN_MINUTES,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getFailedScalingActivitiesInGivenDurationByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesInGivenDurationByClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @QueryParam("durationInMinutes") @DefaultValue("60") long durationInMinutes,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns all the failed scaling activities having start_time in the given time interval between startTimeFrom and startTimeUntil.
     * @param clusterName Name of cluster for which we require all the failed scaling activities.
     * @param startTimeFromInEpochMilliSec time in epoch milliseconds after which we want the activities.
     * @param startTimeUntilInEpochMilliSec time in epoch milliseconds till which we want the activities.
     * @return All the scaling activities which have failed in the given duration.
     */
    @GET
    @Path("cluster_name/{clusterName}/failed_activities_in_time_interval")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ApiDescription.ClusterOpDescription.FAILED_SCALING_ACTIVITIES_BETWEEN_TIME_INTERVAL,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getFailedScalingActivitiesBetweenIntervalByClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesBetweenIntervalByClusterName(
            @PathParam("clusterName") @NotNull String clusterName,
            @QueryParam("startTimeFromInEpochMilliSec")  long startTimeFromInEpochMilliSec,
            @QueryParam("startTimeUntilInEpochMilliSec") long startTimeUntilInEpochMilliSec,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    /**
     * Returns all the failed scaling activities having start_time in the given time interval between startTimeFrom and startTimeUntil.
     * @param clusterCrn Crn of cluster for which we require all the failed scaling activities.
     * @param startTimeFromInEpochMilliSec time in epoch milliseconds after which we want the activities.
     * @param startTimeUntilInEpochMilliSec time in epoch milliseconds till which we want the activities.
     * @return All the scaling activities which have failed in the given duration.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}/failed_activities_in_time_interval")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ApiDescription.ClusterOpDescription.FAILED_SCALING_ACTIVITIES_BETWEEN_TIME_INTERVAL,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getFailedScalingActivitiesBetweenIntervalByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getFailedScalingActivitiesBetweenIntervalByClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @QueryParam("startTimeFromInEpochMilliSec")  long startTimeFromInEpochMilliSec,
            @QueryParam("startTimeUntilInEpochMilliSec") long startTimeUntilInEpochMilliSec,
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
    @Operation(summary = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_BETWEEN_TIME_INTERVAL,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getScalingActivitiesBetweenIntervalByClusterName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesBetweenIntervalByClusterName(
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
    @Operation(summary = ApiDescription.ClusterOpDescription.SCALING_ACTIVITIES_BETWEEN_TIME_INTERVAL,
            description = ApiDescription.DistroXClusterNotes.NOTES,
            operationId = "getScalingActivitiesBetweenIntervalByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Page<DistroXAutoscaleScalingActivityResponse> getScalingActivitiesBetweenIntervalByClusterCrn(
            @PathParam("clusterCrn") @NotNull String clusterCrn,
            @QueryParam("startTimeFromInEpochMilliSec") long startTimeFromInEpochMilliSec,
            @QueryParam("startTimeUntilInEpochMilliSec") long startTimeUntilInEpochMilliSec,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

}
