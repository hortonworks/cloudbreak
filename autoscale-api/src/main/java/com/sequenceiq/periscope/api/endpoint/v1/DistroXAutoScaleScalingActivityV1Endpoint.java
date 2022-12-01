package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.TRIGGER_DESCRIPTION;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/scaling_operations")
@Api(value = "/v1/scaling_operations", description = TRIGGER_DESCRIPTION, protocols = "http,https")
public interface DistroXAutoScaleScalingActivityV1Endpoint {

    /**
     * This Api is used to get all the scaling triggers which are available for a particular cluster.
     * @param clusterName Name of cluster for which we require all the scaling triggers.
     * @return All the scaling triggers available for a particular cluster.
     */
    @GET
    @Path("cluster_name/{clusterName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.TRIGGER_GET_ALL,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getAllScalingTriggersByClusterName(@PathParam("clusterName") String clusterName);

    /**
     * This Api is used to get all the scaling triggers which are available for a particular cluster.
     * @param clusterCrn Crn of cluster for which we require all the scaling triggers.
     * @return All the scaling triggers available for a particular cluster.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.TRIGGER_GET_ALL,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getAllScalingTriggersByClusterCrn(@PathParam("clusterCrn") String clusterCrn);

    /**
     * Returns Scaling triggers after a particular time stamp value.
     * @param clusterName Name of cluster for which we require the scaling triggers.
     * @param durationInMinutes the duration in which we want to get all the scaling triggers.
     * @return List of scaling triggers after the input time stamp value.
     */
    @GET
    @Path("cluster_name/{clustername}/duration/{duration}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.TRIGGER_TIMESTAMP,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInDurationByClusterName(@PathParam("clustername") String clusterName,
            @PathParam("duration") long durationInMinutes);

    /**
     * Returns Scaling triggers after a particular time stamp value.
     * @param clusterCrn Crn of cluster for which we require the scaling triggers.
     * @param durationInMinutes the duration in which we want to get all the scaling triggers.
     * @return List of scaling triggers after the input time stamp value.
     */
    @GET
    @Path("cluster_crn/{clustercrn}/duration/{duration}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.TRIGGER_TIMESTAMP,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInDurationByClusterCrn(@PathParam("clustercrn") String clusterCrn,
            @PathParam("duration") long durationInMinutes);

    /**
     * Returns Scaling trigger for a particular trigger crn value.
     * @param triggerCrn the value for which we want the scaling trigger value.
     * @return Aparticular scaling trigger value associated with the input crn value.
     */
    @GET
    @Path("trigger_crn/{triggerCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.TRIGGER_CRN,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    DistroXAutoscaleScalingActivityResponse getParticularScalingTrigger(@PathParam("triggerCrn") String triggerCrn);

    /**
     * Returns All the failed scaling triggers after a particalur time stmp value.
     * @param clusterName Name of cluster for which we require the scaling triggers.
     * @param durationInMinutes the duration in which we want to get all the failed scaling triggers.
     * @return All the scaling triggers which have failed after the start time value.
     */
    @GET
    @Path("cluster_name/{clusterName}/failed_triggers/duration/{duration}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.FAILED_SCALING_TRIGGERS_AFTER_TIMESTAMP,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getFailedScalingTriggersInGivenDurationByClusterName(@PathParam("clusterName") String clusterName,
            @PathParam("duration") long durationInMinutes);

    /**
     * Returns All the failed scaling triggers after a particalur time stmp value.
     * @param clusterCrn Crn of cluster for which we require the scaling triggers.
     * @param durationInMinutes the duration in which we want to get all the failed scaling triggers.
     * @return All the scaling triggers which have failed after the start time value.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}/failed_triggers/duration/{duration}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.FAILED_SCALING_TRIGGERS_AFTER_TIMESTAMP,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getFailedScalingTriggersInGivenDurationByClusterCrn(@PathParam("clusterCrn") String clusterCrn,
            @PathParam("duration") long durationInMinutes);

    /**
     * Returns all the scaling trigger in the given time interval.
     * @param clusterName Name of cluster for which we require the scaling triggers.
     * @param starttime time after which we want the triggers.
     * @param endTime time till which we want the triggers.
     * @return All the scaling triggers between the start and end time.
     */
    @GET
    @Path("cluster_name/{clusterName}/start_time/{startTime}/end_time/{endTime}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_TRIGGERS_BETWEEN_TIME_INTERVAL,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInTimeRangeByClusterName(@PathParam("clusterName") String clusterName,
            @PathParam("startTime") long startTime, @PathParam("endTime") long endTime);

    /**
     * Returns all the scaling trigger in the given time interval.
     * @param clusterCrn Crn of cluster for which we require the scaling triggers.
     * @param starttime time after which we want the triggers.
     * @param endTime time till which we want the triggers.
     * @return All the scaling triggers between the start and end time.
     */
    @GET
    @Path("cluster_crn/{clusterCrn}/start_time/{startTime}/end_time/{endTime}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ClusterOpDescription.SCALING_TRIGGERS_BETWEEN_TIME_INTERVAL,
            produces = MediaType.APPLICATION_JSON, notes = ApiDescription.DistroXClusterNotes.NOTES)
    List<DistroXAutoscaleScalingActivityResponse> getScalingTriggersInTimeRangeByClusterCrn(@PathParam("clusterCrn") String clusterCrn,
            @PathParam("startTime") long startTime, @PathParam("endTime") long endTime);

}