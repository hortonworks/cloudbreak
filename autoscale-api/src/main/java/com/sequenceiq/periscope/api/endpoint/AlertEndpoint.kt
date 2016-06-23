package com.sequenceiq.periscope.api.endpoint

import com.sequenceiq.periscope.doc.ApiDescription.ALERT_DESCRIPTION
import com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.METRIC_BASED_NOTES
import com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.TIME_BASED_NOTES
import com.sequenceiq.periscope.doc.ApiDescription.JSON

import java.text.ParseException

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

import com.sequenceiq.periscope.api.model.MetricAlertJson
import com.sequenceiq.periscope.api.model.TimeAlertJson
import com.sequenceiq.periscope.doc.ApiDescription.AlertOpDescription

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/clusters/{clusterId}/alerts")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/alerts", description = ALERT_DESCRIPTION, position = 1)
interface AlertEndpoint {

    @POST
    @Path("metric")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_POST, produces = JSON, notes = METRIC_BASED_NOTES)
    fun createAlerts(@PathParam(value = "clusterId") clusterId: Long?, @Valid json: MetricAlertJson): MetricAlertJson

    @PUT
    @Path("metric/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_PUT, produces = JSON, notes = METRIC_BASED_NOTES)
    fun updateAlerts(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "alertId") alertId: Long?,
                     @Valid json: MetricAlertJson): MetricAlertJson

    @GET
    @Path("metric")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_GET, produces = JSON, notes = METRIC_BASED_NOTES)
    fun getAlerts(@PathParam(value = "clusterId") clusterId: Long?): List<MetricAlertJson>

    @DELETE
    @Path(value = "metric/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_DELETE, produces = JSON, notes = METRIC_BASED_NOTES)
    fun deleteAlarm(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "alertId") alertId: Long?)

    @GET
    @Path(value = "metric/definitions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_DEFINITIONS, produces = JSON, notes = METRIC_BASED_NOTES)
    fun getAlertDefinitions(@PathParam(value = "clusterId") clusterId: Long?): List<Map<String, Any>>

    @POST
    @Path("time")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_POST, produces = JSON, notes = TIME_BASED_NOTES)
    @Throws(ParseException::class)
    fun createTimeAlert(@PathParam(value = "clusterId") clusterId: Long?, @Valid json: TimeAlertJson): TimeAlertJson

    @PUT
    @Path("time/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_PUT, produces = JSON, notes = TIME_BASED_NOTES)
    @Throws(ParseException::class)
    fun setTimeAlert(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "alertId") alertId: Long?, @Valid json: TimeAlertJson): TimeAlertJson

    @GET
    @Path("time")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_GET, produces = JSON, notes = TIME_BASED_NOTES)
    fun getTimeAlerts(@PathParam(value = "clusterId") clusterId: Long?): List<TimeAlertJson>

    @DELETE
    @Path("time/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_DELETE, produces = JSON, notes = TIME_BASED_NOTES)
    fun deleteTimeAlert(@PathParam(value = "clusterId") clusterId: Long?, @PathParam(value = "alertId") alertId: Long?)
}
