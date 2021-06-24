package com.sequenceiq.cloudbreak.api.endpoint;


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

import static com.sequenceiq.cloudbreak.doc.Notes.CUSTOM_CONFIGS_NOTES;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.requests.CloneCustomConfigsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.requests.CustomConfigsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.responses.CustomConfigsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.responses.CustomConfigsV4Responses;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomConfigsOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/customconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/customconfigs", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface CustomConfigsV4Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.GET_ALL, produces = MediaType.APPLICATION_JSON, nickname = "list",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Responses list();

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON, nickname = "getByCrn",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response getByCrn(@PathParam("crn") @NotNull String crn);

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, nickname = "getByName",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response getByName(@PathParam("name") @NotNull String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, nickname = "post", notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response post(@Valid CustomConfigsV4Request request);

    @POST
    @Path("/name/{name}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.CLONE_BY_NAME, produces = MediaType.APPLICATION_JSON, nickname = "cloneByName",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response cloneByName(@PathParam("name") String name, @Valid CloneCustomConfigsV4Request cloneCustomConfigsRequest);

    @POST
    @Path("/crn/{crn}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.CLONE_BY_CRN, produces = MediaType.APPLICATION_JSON, nickname = "cloneByCrn",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response cloneByCrn(@PathParam("crn") String crn, @Valid CloneCustomConfigsV4Request cloneCustomConfigsRequest);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON, nickname = "deleteByCrn",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response deleteByCrn(@PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, nickname = "deleteByName",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Response deleteByName(@PathParam("name") @NotNull String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CustomConfigsOpDescription.DELETE_MULTIPLE, produces = MediaType.APPLICATION_JSON, nickname = "deleteMultiple",
            notes = CUSTOM_CONFIGS_NOTES)
    CustomConfigsV4Responses deleteMultiple(Set<String> names);
}
