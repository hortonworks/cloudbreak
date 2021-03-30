package com.sequenceiq.cloudbreak.api.endpoint.v4;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.sequenceiq.cloudbreak.doc.Notes.CUSTOM_CONFIGURATIONS_NOTES;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CloneCustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Responses;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomConfigurationsOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/custom_configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/custom_configurations", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface CustomConfigurationsV4Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.GET_ALL, produces = MediaType.APPLICATION_JSON, nickname = "list",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Responses list();

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON, nickname = "getByCrn",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response getByCrn(@PathParam("crn") @NotNull String crn);

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, nickname = "getByName",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response getByName(@PathParam("name") @NotNull String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, nickname = "post",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response post(@Valid CustomConfigurationsV4Request request);

    @POST
    @Path("/name/{name}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.CLONE_BY_NAME, produces = MediaType.APPLICATION_JSON, nickname = "cloneByName",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response cloneByName(@PathParam("name") @NotNull String name, @Valid CloneCustomConfigurationsV4Request cloneCustomConfigsRequest);

    @POST
    @Path("/crn/{crn}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.CLONE_BY_CRN, produces = MediaType.APPLICATION_JSON, nickname = "cloneByCrn",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response cloneByCrn(@PathParam("crn") @NotNull String crn, @Valid CloneCustomConfigurationsV4Request cloneCustomConfigsRequest);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON, nickname = "deleteByCrn",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response deleteByCrn(@PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigurationsOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, nickname = "deleteByName",
            notes = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response deleteByName(@PathParam("name") @NotNull String name);
}
