package com.sequenceiq.redbeams.api.endpoint.v4.database;

import static com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseOpDescription;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.doc.ControllerDescriptions;
import com.sequenceiq.redbeams.doc.Notes;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/databases")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/databases", description = ControllerDescriptions.DATABASE_V4_DESCRIPTION, protocols = "http,https")
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "listDatabases")
    DatabaseV4Responses list(@NotNull @QueryParam("environmentId") String environmentId);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "createDatabase")
    DatabaseV4Response create(@Valid DatabaseV4Request request);

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.REGISTER, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "registerDatabase")
    DatabaseV4Response register(@Valid DatabaseV4Request request);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "getDatabase")
    DatabaseV4Response get(@NotNull @QueryParam("environmentId") String environmentId, @PathParam("name") String name);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "deleteDatabase")
    DatabaseV4Response delete(@NotNull @QueryParam("environmentId") String environmentId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "deleteMultipleDatabases")
    DatabaseV4Responses deleteMultiple(@NotNull @QueryParam("environmentId") String environmentId, Set<String> names);

    // @GET
    // @Path("/{name}/request")
    // @Produces(MediaType.APPLICATION_JSON)
    // @ApiOperation(value = DatabaseOpDescription.GET_REQUEST, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
    //         nickname = "getDatabaseRequestFromName")
    // DatabaseV4Request getRequest(@PathParam("name") String name);

    @POST
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.POST_CONNECTION_TEST, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "testDatabaseConnection")
    DatabaseTestV4Response test(@Valid DatabaseTestV4Request databaseTestV4Request);
}
