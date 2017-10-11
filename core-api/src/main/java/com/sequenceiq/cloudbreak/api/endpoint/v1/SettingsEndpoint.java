package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SettingsOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/settings")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/settings", description = ControllerDescription.SETTINGS_DESCRIPTION, protocols = "http,https")
public interface SettingsEndpoint {

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SettingsOpDescription.GET_ALL_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES,
            nickname = "getAllSettings")
    Map<String, Map<String, Object>> getAllSettings();

    @GET
    @Path("recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SettingsOpDescription.GET_RECIPE_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES,
            nickname = "getRecipeSettings")
    Map<String, Object> getRecipeSettings();

    @GET
    @Path("database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SettingsOpDescription.GET_DATABASE_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES,
            nickname = "getDatabaseConfigSettings")
    Map<String, Object> getDatabaseConfigSettings();
}
