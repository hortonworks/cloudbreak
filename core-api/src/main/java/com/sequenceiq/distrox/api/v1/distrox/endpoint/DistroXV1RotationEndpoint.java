package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATALAKE;
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

import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMultiSecretRotationRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface DistroXV1RotationEndpoint {

    @PUT
    @Path("rotate_secret")
    @ApiOperation(value = "Rotate DistroX secrets", produces = MediaType.APPLICATION_JSON, nickname = "rotateDistroXSecrets")
    FlowIdentifier rotateSecrets(@Valid @NotNull DistroXSecretRotationRequest request);

    @PUT
    @Path("multi_secret/rotate")
    @ApiOperation(value = "Rotate DistroX multi secrets", produces = MediaType.APPLICATION_JSON, nickname = "rotateDistroXMultiSecrets")
    FlowIdentifier rotateMultiSecrets(@Valid @NotNull DistroXMultiSecretRotationRequest request);

    @GET
    @Path("multi_secret/check")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check ongoing child DistroX multi secret rotations", produces = MediaType.APPLICATION_JSON, nickname = "checkDistroXMultiSecrets")
    boolean checkOngoingChildrenMultiSecretRotations(@ValidCrn(resource = { ENVIRONMENT, DATALAKE }) @QueryParam("parentCrn") String parentCrn,
            @ValidMultiSecretType @QueryParam("secret") String secret,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("multi_secret/mark_resources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Mark child resources for DistroX multi secret rotations", produces = MediaType.APPLICATION_JSON,
            nickname = "markResourcesDistroXMultiSecrets")
    void markMultiClusterChildrenResources(@ValidCrn(resource = { ENVIRONMENT, DATALAKE }) @QueryParam("parentCrn") String parentCrn,
            @ValidMultiSecretType @QueryParam("secret") String secret,
            @InitiatorUserCrn @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
