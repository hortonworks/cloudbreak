package com.sequenceiq.cloudbreak.api;


import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;
import com.sequenceiq.cloudbreak.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.model.CertificateResponse;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.model.StackRequest;
import com.sequenceiq.cloudbreak.model.StackResponse;
import com.sequenceiq.cloudbreak.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.model.UpdateStackJson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/stack", description = ControllerDescription.STACK_DESCRIPTION, position = 3)
public interface StackEndpoint {

    @POST
    @Path("user/stacks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    IdJson postPrivate(StackRequest stackRequest);

    @POST
    @Path("account/stacks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    IdJson postPublic(StackRequest stackRequest);

    @POST
    @Path("stacks/validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    Response validate(StackValidationRequest stackValidationRequest);

    @POST
    @Path(value = "stacks/ambari")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    StackResponse getStackForAmbari(AmbariAddressJson json);

    @GET
    @Path("user/stacks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    Set<StackResponse> getPrivates();

    @GET
    @Path("account/stacks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    Set<StackResponse> getPublics();

    @GET
    @Path("user/stacks/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    StackResponse getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/stacks/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    StackResponse getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("stacks/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    StackResponse get(@PathParam(value = "id") Long id);

    @GET
    @Path(value = "stacks/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STATUS_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    Map<String, Object> status(@PathParam(value = "id") Long id);

    @GET
    @Path(value = "stacks/platformVariants")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_PLATFORM_VARIANTS, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    PlatformVariantsJson variants();

    @DELETE
    @Path("stacks/{id}")
    @ApiOperation(value = StackOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    void delete(@PathParam(value = "id") Long id, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("account/stacks/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    void deletePublic(@PathParam(value = "name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("user/stacks/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    void deletePrivate(@PathParam(value = "name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("stacks/{stackId}/{instanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_INSTANCE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    Response deleteInstance(@PathParam(value = "stackId") Long stackId, @PathParam(value = "instanceId") String instanceId);

    @PUT
    @Path("stacks/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    Response put(@PathParam(value = "id")Long id, UpdateStackJson updateRequest);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = ContentType.JSON, notes = Notes.STACK_NOTES)
    @Path(value = "stacks/{id}/certificate")
    CertificateResponse getCertificate(@PathParam(value = "id") Long stackId);
}
