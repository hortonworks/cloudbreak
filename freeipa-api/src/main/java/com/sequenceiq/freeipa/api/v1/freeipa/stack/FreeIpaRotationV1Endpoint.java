package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.ROTATE_MULTI_SECRETS_BY_CRN;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.ROTATE_SECRETS_BY_CRN;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaMultiSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RetryAndMetrics
@Path("/v1/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa",
        protocols = "http,https",
        authorizations = {@Authorization(value = FreeIpaApi.CRN_HEADER_API_KEY)},
        consumes = MediaType.APPLICATION_JSON)
public interface FreeIpaRotationV1Endpoint {

    @PUT
    @Path("rotate_secret")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ROTATE_SECRETS_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "rotateSecretsByCrn")
    FlowIdentifier rotateSecretsByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @Valid @NotNull FreeIpaSecretRotationRequest request);

    @PUT
    @Path("multi_secret/rotate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ROTATE_MULTI_SECRETS_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "rotateMultiSecretsByCrn")
    FlowIdentifier rotateMultiSecretsByCrn(@Valid @NotNull FreeIpaMultiSecretRotationRequest request,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
