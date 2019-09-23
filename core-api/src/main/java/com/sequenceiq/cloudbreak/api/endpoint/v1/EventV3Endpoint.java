package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.List;

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

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EventOpDescription;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/events")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/events", description = ControllerDescription.EVENT_DESCRIPTION, protocols = "http,https")
public interface EventV3Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_BY_TIMESTAMP, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEventsInWorkspace")
    List<CloudbreakEventsJson> getCloudbreakEventsSince(@PathParam("workspaceId") Long workspaceId, @QueryParam("since") Long since);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getEventsByStackNameInWorkspace")
    Page<CloudbreakEventsJson> getCloudbreakEventsByStack(
            @PathParam("workspaceId") Long workspaceId,
            @PathParam("name") String name,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("100") Integer size);

    @GET
    @Path("struct/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EventOpDescription.GET_EVENTS_BY_NAME, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getStructuredEventsInWorkspace")
    StructuredEventContainer getStructuredEvents(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("struct/zip/{name}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = EventOpDescription.GET_EVENTS_ZIP_BY_NAME, produces = ContentType.JSON, notes = Notes.EVENT_NOTES,
            nickname = "getStructuredEventsZipInWorkspace")
    Response getStructuredEventsZip(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
