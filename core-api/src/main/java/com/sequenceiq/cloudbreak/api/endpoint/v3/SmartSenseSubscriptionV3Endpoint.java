package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.Notes.SMARTSENSE_SUBSCRIPTION_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SmartSenseSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/smartsensesubscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/smartsensesubscriptions", description = ControllerDescription.SMARTSENSE_SUBSCRIPTION_V3_DESCRIPTION,
        protocols = "http,https")
public interface SmartSenseSubscriptionV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "listSmartSenseSubscriptionsByOrganization")
    Set<SmartSenseSubscriptionJson> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_DEFAULT_IN_ORG, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getDefaultSmartSenseSubscriptionInOrganization")
    SmartSenseSubscriptionJson getDefaultInOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscriptionInOrganization")
    SmartSenseSubscriptionJson getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "createSmartSenseSubscriptionInOrganization")
    SmartSenseSubscriptionJson createInOrganization(@PathParam("organizationId") Long organizationId, @Valid SmartSenseSubscriptionJson request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "deleteSmartSenseSubscriptionInOrganization")
    SmartSenseSubscriptionJson deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}
