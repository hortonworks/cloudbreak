package com.sequenceiq.cloudbreak.api.endpoint.v4;

import static com.sequenceiq.cloudbreak.doc.ApiDescription.CUSTOM_CONFIGURATIONS_NOTES;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CloneCustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.RoleTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.ServiceTypeV4Response;
import com.sequenceiq.cloudbreak.doc.ApiDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RetryAndMetrics
@Path("/v4/custom_configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/custom_configurations")
public interface CustomConfigurationsV4Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.GET_ALL, operationId ="list",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Responses list();

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.GET_BY_CRN, operationId ="getByCrn",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response getByCrn(@PathParam("crn") @NotNull String crn);

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.GET_BY_NAME, operationId ="getByName",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response getByName(@PathParam("name") @NotNull String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.CREATE, operationId ="post",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response post(@Valid CustomConfigurationsV4Request request);

    @POST
    @Path("/name/{name}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.CLONE_BY_NAME, operationId ="cloneByName",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response cloneByName(@PathParam("name") @NotNull String name, @Valid CloneCustomConfigurationsV4Request cloneCustomConfigsRequest);

    @POST
    @Path("/crn/{crn}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.CLONE_BY_CRN, operationId ="cloneByCrn",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response cloneByCrn(@PathParam("crn") @NotNull String crn, @Valid CloneCustomConfigurationsV4Request cloneCustomConfigsRequest);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.DELETE_BY_CRN, operationId ="deleteByCrn",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response deleteByCrn(@PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.DELETE_BY_NAME, operationId ="deleteByName",
            description =  CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response deleteByName(@PathParam("name") @NotNull String name);

    @GET
    @Path("/service_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.GET_SERVICE_TYPES,
            operationId = "getServiceTypes")
    ServiceTypeV4Response getServiceTypes();

    @GET
    @Path("/role_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ApiDescription.CustomConfigurationsOpDescription.GET_ROLE_TYPES, operationId ="getRoleTypes")
    RoleTypeV4Response getRoleTypes();
}
