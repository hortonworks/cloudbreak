package com.sequenceiq.freeipa.api.v1.freeipa.upgrade;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RetryAndMetrics
@Path("/v1/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa")
public interface FreeIpaUpgradeV1Endpoint {

    @POST
    @Path("/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  FreeIpaOperationDescriptions.UPGRADE_FREEIPA, description =  FreeIpaNotes.FREEIPA_NOTES,
            operationId = "upgradeFreeIpaV1")
    FreeIpaUpgradeResponse upgradeFreeIpa(@Valid FreeIpaUpgradeRequest request);

    @GET
    @Path("/upgrade/options")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  FreeIpaOperationDescriptions.UPGRADE_OPTIONS, description =  FreeIpaNotes.FREEIPA_NOTES,
            operationId = "getFreeIpaUpgradeOptionsV1")
    FreeIpaUpgradeOptions getFreeIpaUpgradeOptions(
            @QueryParam("environmentCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn,
            @QueryParam("catalog") String catalog);
}
