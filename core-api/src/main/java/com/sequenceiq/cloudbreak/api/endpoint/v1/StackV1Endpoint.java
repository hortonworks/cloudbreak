package com.sequenceiq.cloudbreak.api.endpoint.v1;


import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
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

import com.sequenceiq.cloudbreak.api.endpoint.common.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/stacks", description = ControllerDescription.STACK_DESCRIPTION, protocols = "http,https")
public interface StackV1Endpoint extends StackEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "postPrivateStack")
    StackResponse postPrivate(@Valid StackRequest stackRequest) throws Exception;

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "postPublicStack")
    StackResponse postPublic(@Valid StackRequest stackRequest) throws Exception;

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getPrivatesStack")
    Set<StackResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getPublicsStack")
    Set<StackResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getPrivateStack")
    StackResponse getPrivate(@PathParam("name") String name, @QueryParam("entry") Set<String> entries);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getPublicStack")
    StackResponse getPublic(@PathParam("name") String name, @QueryParam("entry") Set<String> entries);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getStack")
    StackResponse get(@PathParam("id") Long id, @QueryParam("entry") Set<String> entries);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "deletePublicStack")
    void deletePublic(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "deletePrivateStack")
    void deletePrivate(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "deleteStack")
    void delete(@PathParam("id") Long id, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "putStack")
    Response put(@PathParam("id") Long id, @Valid UpdateStackJson updateRequest);

    @GET
    @Path("{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_STATUS_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "statusStack")
    Map<String, Object> status(@PathParam("id") Long id);

    @GET
    @Path("platformVariants")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_PLATFORM_VARIANTS, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "variantsStack")
    PlatformVariantsJson variants();

    @DELETE
    @Path("{stackId}/{instanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.DELETE_INSTANCE_BY_ID, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "deleteInstanceStack")
    Response deleteInstance(@PathParam("stackId") Long stackId, @PathParam("instanceId") String instanceId);

    @GET
    @Path("{id}/certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getCertificateStack")
    CertificateResponse getCertificate(@PathParam("id") Long stackId);

    @POST
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.VALIDATE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "validateStack")
    Response validate(@Valid StackValidationRequest stackValidationRequest);

    @POST
    @Path("ambari")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_BY_AMBARI_ADDRESS, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getStackForAmbari")
    StackResponse getStackForAmbari(@Valid AmbariAddressJson json);

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    @ApiOperation(value = StackOpDescription.GET_ALL, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getAllStack")
    Set<AutoscaleStackResponse> getAllForAutoscale();

}
