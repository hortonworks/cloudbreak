package com.sequenceiq.cloudbreak.api.endpoint

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/settings")
@Consumes(MediaType.APPLICATION_JSON)
interface SettingsEndpoint {

    val allSettings: Map<String, Map<String, Any>>

    val recipeSettings: Map<String, Any>

    val sssdConfigSettings: Map<String, Any>
}
