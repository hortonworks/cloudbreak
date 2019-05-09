package com.sequenceiq.redbeams.api.endpoint.v4.database;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.redbeams.doc.ControllerDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Set;

@Path("/v4/databases")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/databases", description = ControllerDescriptions.DATABASE_V4_DESCRIPTION, protocols = "http,https")
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.LIST, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "listDatabases")
    DatabaseV4Responses list(@QueryParam("environment") String environment,
            @QueryParam("attachGlobal") @DefaultValue("false") Boolean attachGlobal);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.CREATE, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "createDatabase")
    DatabaseV4Response create(@Valid DatabaseV4Request request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "getDatabase")
    DatabaseV4Response get(@PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.DELETE_BY_NAME, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "deleteDatabase")
    DatabaseV4Response delete(@PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "deleteDatabases")
    DatabaseV4Responses deleteMultiple(Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.GET_REQUEST, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "getDatabaseRequestFromName")
    DatabaseV4Request getRequest(@PathParam("name") String name);

    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.POST_CONNECTION_TEST, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "testDatabaseConnection")
    DatabaseTestV4Response test(@Valid DatabaseTestV4Request databaseTestV4Request);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "attachDatabaseToEnvironments")
    DatabaseV4Response attach(@PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.DatabaseOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
            nickname = "detachDatabaseFromEnvironments")
    DatabaseV4Response detach(@PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);
}
