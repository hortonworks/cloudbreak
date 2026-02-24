package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/internal/env")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/internal/env")
public interface EnvironmentInternalEndpoint {

    @GET
    @Path("/crn/{crn}/policy_validation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.POLICY_VALIDATION_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "policyValidationInternalByEnvironmentCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PolicyValidationErrorResponses policyValidationByEnvironmentCrn(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
        @QueryParam("service") List<String> services);

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.INTERNAL_CONSUMPTION_LIST, description = ENVIRONMENT_NOTES,
            operationId = "getEnvironmentV1InternalByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleEnvironmentResponse internalGetByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
        @QueryParam("withNetwork") @DefaultValue("false") boolean withNetwork);

    @POST
    @Path("/crn/{crn}/create_distribution_list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CREATE_DISTRIBUTION_LIST, description = ENVIRONMENT_NOTES,
            operationId = "createDistributionListByEnvironmentCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void createOrUpdateDistributionListByEnvironmentCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

}
