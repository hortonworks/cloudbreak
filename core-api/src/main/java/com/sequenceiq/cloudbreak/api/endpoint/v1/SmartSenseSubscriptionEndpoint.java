package com.sequenceiq.cloudbreak.api.endpoint.v1;

import static com.sequenceiq.cloudbreak.doc.Notes.SMARTSENSE_SUBSCRIPTION_NOTES;

import java.util.List;

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
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SmartSenseSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/smartsensesubscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/smartsensesubscriptions", description = ControllerDescription.SMARTSENSE_SUBSCRIPTION_DESCRIPTION, protocols = "http,https")
public interface SmartSenseSubscriptionEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscription")
    SmartSenseSubscriptionJson get();

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getSmartSenseSubscriptionById")
    SmartSenseSubscriptionJson get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "deleteSmartSenseSubscriptionById")
    void delete(@PathParam("id") Long id);

    @DELETE
    @Path("account/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.DELETE_PUBLIC_BY_ID, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "deletePublicSmartSenseSubscriptionBySubscriptionId")
    void deletePublic(@PathParam("subscriptionId") String subscriptionId);

    @DELETE
    @Path("user/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.DELETE_PRIVATE_BY_ID, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "deletePrivateSmartSenseSubscriptionBySubscriptionId")
    void deletePrivate(@PathParam("subscriptionId") String subscriptionId);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "postPublicSmartSenseSubscription")
    SmartSenseSubscriptionJson postPublic(@Valid SmartSenseSubscriptionJson smartSenseSubscription);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getPublicSmartSenseSubscriptions")
    List<SmartSenseSubscriptionJson> getPublics();

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "postPrivateSmartSenseSubscription")
    SmartSenseSubscriptionJson postPrivate(@Valid SmartSenseSubscriptionJson smartSenseSubscription);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SmartSenseSubOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = SMARTSENSE_SUBSCRIPTION_NOTES,
            nickname = "getPrivateSmartSenseSubscriptions")
    List<SmartSenseSubscriptionJson> getPrivates();
}
