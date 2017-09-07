package com.sequenceiq.periscope.api.endpoint;

import static com.sequenceiq.periscope.doc.ApiDescription.ALERT_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.METRIC_BASED_NOTES;
import static com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.PROMETHEUS_BASED_NOTES;
import static com.sequenceiq.periscope.doc.ApiDescription.AlertNotes.TIME_BASED_NOTES;
import static com.sequenceiq.periscope.doc.ApiDescription.AlertOpDescription.PROMETHEUS_BASED_DEFINITIONS;
import static com.sequenceiq.periscope.doc.ApiDescription.JSON;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

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

import com.sequenceiq.periscope.api.model.AlertRuleDefinitionEntry;
import com.sequenceiq.periscope.api.model.MetricAlertJson;
import com.sequenceiq.periscope.api.model.PrometheusAlertJson;
import com.sequenceiq.periscope.api.model.TimeAlertJson;
import com.sequenceiq.periscope.doc.ApiDescription.AlertOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clusters/{clusterId}/alerts")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/alerts", description = ALERT_DESCRIPTION, protocols = "http,https")
public interface AlertEndpoint {

    @POST
    @Path("metric")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_POST, produces = JSON, notes = METRIC_BASED_NOTES)
    MetricAlertJson createAlerts(@PathParam("clusterId") Long clusterId, @Valid MetricAlertJson json);

    @PUT
    @Path("metric/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_PUT, produces = JSON, notes = METRIC_BASED_NOTES)
    MetricAlertJson updateAlerts(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId,
            @Valid MetricAlertJson json);

    @GET
    @Path("metric")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_GET, produces = JSON, notes = METRIC_BASED_NOTES)
    List<MetricAlertJson> getAlerts(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("metric/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_DELETE, produces = JSON, notes = METRIC_BASED_NOTES)
    void deleteAlarm(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId);

    @GET
    @Path("metric/definitions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_DEFINITIONS, produces = JSON, notes = METRIC_BASED_NOTES)
    List<Map<String, Object>> getAlertDefinitions(@PathParam("clusterId") Long clusterId);

    @POST
    @Path("time")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_POST, produces = JSON, notes = TIME_BASED_NOTES)
    TimeAlertJson createTimeAlert(@PathParam("clusterId") Long clusterId, @Valid TimeAlertJson json) throws ParseException;

    @PUT
    @Path("time/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_PUT, produces = JSON, notes = TIME_BASED_NOTES)
    TimeAlertJson setTimeAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId, @Valid TimeAlertJson json)
            throws ParseException;

    @GET
    @Path("time")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_GET, produces = JSON, notes = TIME_BASED_NOTES)
    List<TimeAlertJson> getTimeAlerts(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("time/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.TIME_BASED_DELETE, produces = JSON, notes = TIME_BASED_NOTES)
    void deleteTimeAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId);

    @POST
    @Path("prometheus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.PROMETHEUS_BASED_POST, produces = JSON, notes = PROMETHEUS_BASED_NOTES)
    PrometheusAlertJson createPrometheusAlert(@PathParam("clusterId") Long clusterId, @Valid PrometheusAlertJson json);

    @PUT
    @Path("prometheus/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.PROMETHEUS_BASED_PUT, produces = JSON, notes = PROMETHEUS_BASED_NOTES)
    PrometheusAlertJson updatePrometheusAlert(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId,
            @Valid PrometheusAlertJson json);

    @GET
    @Path("prometheus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.PROMETHEUS_BASED_GET, produces = JSON, notes = PROMETHEUS_BASED_NOTES)
    List<PrometheusAlertJson> getPrometheusAlerts(@PathParam("clusterId") Long clusterId);

    @DELETE
    @Path("prometheus/{alertId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.PROMETHEUS_BASED_DELETE, produces = JSON, notes = PROMETHEUS_BASED_NOTES)
    void deletePrometheusAlarm(@PathParam("clusterId") Long clusterId, @PathParam("alertId") Long alertId);

    @GET
    @Path("prometheus/definitions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AlertOpDescription.METRIC_BASED_DEFINITIONS, produces = JSON, notes = PROMETHEUS_BASED_DEFINITIONS)
    List<AlertRuleDefinitionEntry> getPrometheusDefinitions(@PathParam("clusterId") Long clusterId);
}
