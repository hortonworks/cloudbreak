package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxChildResourceMarkingRequest;
import com.sequenceiq.sdx.api.model.SdxSecretRotationRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxRotationEndpoint {

    @PUT
    @Path("rotate_secret")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Rotate SDX secrets", produces = MediaType.APPLICATION_JSON, nickname = "rotateSDXSecrets")
    FlowIdentifier rotateSecrets(@Valid @NotNull SdxSecretRotationRequest request);

    @GET
    @Path("multi_secret/check_children")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check ongoing child SDX multi secret rotations by parent", produces = MediaType.APPLICATION_JSON,
            nickname = "checkSDXMultiSecretsByParent")
    boolean checkOngoingMultiSecretChildrenRotationsByParent(@ValidCrn(resource = ENVIRONMENT) @QueryParam("parentCrn") String parentCrn,
            @ValidMultiSecretType @QueryParam("secret") String multiSecret,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("multi_secret/mark_children")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Mark child resources for SDX multi secret rotations by parent", produces = MediaType.APPLICATION_JSON,
            nickname = "markResourcesSDXMultiSecretsByParent")
    void markMultiClusterChildrenResourcesByParent(@Valid SdxChildResourceMarkingRequest request,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}