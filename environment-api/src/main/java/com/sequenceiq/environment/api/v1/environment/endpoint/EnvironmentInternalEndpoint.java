package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/internal/env")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/internal/env", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface EnvironmentInternalEndpoint {

    @GET
    @Path("/crn/{crn}/policy_validation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.POLICY_VALIDATION_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "policyValidationInternalByEnvironmentCrn")
    PolicyValidationErrorResponses policyValidationByEnvironmentCrn(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
        @QueryParam("service") List<String> services);
}
