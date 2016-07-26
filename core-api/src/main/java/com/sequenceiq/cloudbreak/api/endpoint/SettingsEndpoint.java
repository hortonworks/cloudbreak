package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/settings")
@Consumes(MediaType.APPLICATION_JSON)
public interface SettingsEndpoint {

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Map<String, Object>> getAllSettings();

    @GET
    @Path("recipe")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> getRecipeSettings();

    @GET
    @Path("sssd")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> getSssdConfigSettings();

    @GET
    @Path("database")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> getDatabaseConfigSettings();
}
