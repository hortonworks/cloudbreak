package com.sequenceiq.cloudbreak.api.endpoint.v1;

import static com.sequenceiq.cloudbreak.doc.Notes.FLEX_SUBSCRIPTION_NOTES;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.FlexSubOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/flexsubscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/flexsubscriptions", description = ControllerDescription.FLEX_SUBSCRIPTION_DESCRIPTION, protocols = "http,https")
public interface FlexSubscriptionEndpoint {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "getFlexSubscriptionById")
    FlexSubscriptionResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "deleteFlexSubscriptionById")
    void delete(@PathParam("id") Long id);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "postPublicFlexSubscription")
    FlexSubscriptionResponse postPublic(@Valid FlexSubscriptionRequest flexSubscription);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "getPublicFlexSubscriptions")
    List<FlexSubscriptionResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "getPublicFlexSubscriptionByName")
    FlexSubscriptionResponse getPublic(@PathParam("name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "deletePublicFlexSubscriptionByName")
    void deletePublic(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "deletePrivateFlexSubscriptionByName")
    void deletePrivate(@PathParam("name") String name);

    @PUT
    @Path("account/setdefault/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_DEFAULT_IN_ACCOUNT, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putPublicDefaultFlexSubscriptionByName")
    void setDefaultInAccount(@PathParam("name") String name);

    @PUT
    @Path("account/setusedforcontroller/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_USED_FOR_CONTROLLER_IN_ACCOUNT, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putPublicUsedForControllerFlexSubscriptionByName")
    void setUsedForControllerInAccount(@PathParam("name") String name);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "postPrivateFlexSubscription")
    FlexSubscriptionResponse postPrivate(@Valid FlexSubscriptionRequest flexSubscription);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "getPrivateFlexSubscriptions")
    List<FlexSubscriptionResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "getPrivateFlexSubscriptionByName")
    FlexSubscriptionResponse getPrivate(@PathParam("name") String name);

    @PUT
    @Path("setdefault/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_DEFAULT_IN_ACCOUNT, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putDefaultFlexSubscriptionById")
    void setDefaultInAccount(@PathParam("id") Long id);

    @PUT
    @Path("setusedforcontroller/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FlexSubOpDescription.SET_USED_FOR_CONTROLLER_IN_ACCOUNT, produces = ContentType.JSON, notes = FLEX_SUBSCRIPTION_NOTES,
            nickname = "putUsedForControllerFlexSubscriptionById")
    void setUsedForControllerInAccount(@PathParam("id") Long id);
}
