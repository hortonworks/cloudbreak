package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.ALERT_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.LOAD_BASED_NOTES;
import static com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.TIME_BASED_NOTES;

import java.text.ParseException;
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

import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.api.model.TimeAlertValidationRequest;
import com.sequenceiq.periscope.doc.ApiDescription.AlertOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/clusters/{clusterId}/alerts")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/alerts", description = ALERT_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AlertEndpoint {

    @POST
    @Path("time")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_POST, produces = MediaType.APPLICATION_JSON, notes = TIME_BASED_NOTES)
    TimeAlertResponse createTimeAlert(@PathParam("clusterId") Long clusterId, @Valid TimeAlertRequest json);

    @PUT
    @Path("time/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_PUT, produces = MediaType.APPLICATION_JSON, notes = TIME_BASED_NOTES)
    TimeAlertResponse updateTimeAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId, @Valid TimeAlertRequest json);

    @GET
    @Path("time")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_GET, produces = MediaType.APPLICATION_JSON, notes = TIME_BASED_NOTES)
    List<TimeAlertResponse> getTimeAlerts(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("time/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_DELETE, produces = MediaType.APPLICATION_JSON, notes = TIME_BASED_NOTES)
    void deleteTimeAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId);

    @POST
    @Path("time/validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_CRON, produces = MediaType.APPLICATION_JSON, notes = TIME_BASED_NOTES)
    Boolean validateCronExpression(@PathParam("clusterId") Long clusterId, @Valid TimeAlertValidationRequest json) throws ParseException;

    @POST
    @Path("load")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.LOAD_BASED_POST, produces = MediaType.APPLICATION_JSON, notes = LOAD_BASED_NOTES)
    LoadAlertResponse createLoadAlert(@PathParam("clusterId") Long clusterId, @Valid LoadAlertRequest json);

    @PUT
    @Path("load/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.LOAD_BASED_PUT, produces = MediaType.APPLICATION_JSON, notes = LOAD_BASED_NOTES)
    LoadAlertResponse updateLoadAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId, @Valid LoadAlertRequest json);

    @GET
    @Path("load")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.LOAD_BASED_GET, produces = MediaType.APPLICATION_JSON, notes = LOAD_BASED_NOTES)
    List<LoadAlertResponse> getLoadAlerts(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("load/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.LOAD_BASED_DELETE, produces = MediaType.APPLICATION_JSON, notes = LOAD_BASED_NOTES)
    void deleteLoadAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId);
}
