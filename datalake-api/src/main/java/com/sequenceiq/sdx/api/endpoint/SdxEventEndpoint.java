package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.data.domain.Page;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.sdx.api.model.event.SdxEventResponse;
import com.sequenceiq.sdx.api.model.event.SdxEventResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/sdx/event")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/sdx/event", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxEventEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get sdx event from tenant", produces = MediaType.APPLICATION_JSON, nickname = "getEvents")
    SdxEventResponses list(@QueryParam("since") Long since);

    @GET
    @Path("{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get events by sdx crn", produces = MediaType.APPLICATION_JSON, nickname = "getEventsByCrn")
    Page<SdxEventResponse> getEventsByCrn(
            @PathParam("crn") String crn,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("{crn}/structured")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get structured events by crn", produces = MediaType.APPLICATION_JSON, nickname = "getStructuredEvents")
    StructuredEventContainer structured(@PathParam("crn") String crn);

    @GET
    @Path("{crn}/zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "get events zip by sdx crn", produces = MediaType.APPLICATION_JSON, nickname = "getStructuredEventsZip")
    Response download(@PathParam("crn") String crn);
}
