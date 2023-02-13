package com.sequenceiq.environment.api.v1.tags.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.tag.TagDescription;
import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequests;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;
import com.sequenceiq.environment.api.v1.tags.model.response.GeneratedAccountTagResponses;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RetryAndMetrics
@Path("/v1/tags")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/tags")
public interface AccountTagEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  TagDescription.GET, description =  TagDescription.GET_NOTES, operationId ="listTagsV1")
    AccountTagResponses list();

    @GET
    @Path("{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  TagDescription.GET, description =  TagDescription.GET_NOTES, operationId ="listTagsInAccountV1")
    AccountTagResponses listInAccount(@AccountId @PathParam("accountId") String accountId);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  TagDescription.PUT, description =  TagDescription.PUT_NOTES, operationId ="putTagsV1")
    AccountTagResponses put(@Valid AccountTagRequests request);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  TagDescription.GET_GENERATED, description =  TagDescription.GET_GENERATED_NOTES,
            operationId = "getGeneratedTagsV1")
    GeneratedAccountTagResponses generate(@QueryParam("environmentName") String environmentName, @QueryParam("environmentCrn") String environmentCrn);
}
