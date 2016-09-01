package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/settings")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/settings", description = ControllerDescription.SETTINGS_DESCRIPTION)
public interface SettingsEndpoint {

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SettingsOpDescription.GET_ALL_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES)
    Map<String, Map<String, Object>> getAllSettings();

    @GET
    @Path("recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SettingsOpDescription.GET_RECIPE_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES)
    Map<String, Object> getRecipeSettings();

    @GET
    @Path("sssd")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SettingsOpDescription.GET_SSSD_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES)
    Map<String, Object> getSssdConfigSettings();

    @GET
    @Path("database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SettingsOpDescription.GET_DATABASE_SETTINGS, produces = ContentType.JSON, notes = Notes.SETTINGS_NOTES)
    Map<String, Object> getDatabaseConfigSettings();
}
