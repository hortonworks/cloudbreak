package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN_INTERNAL;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v1/internal/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/internal/distrox", protocols = "http,https")
public interface DistroXInternalV1Endpoint {

    @GET
    @Path("crn/{crn}")
    @ApiOperation(value = GET_BY_CRN_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXInternalV1ByCrn")
    StackViewV4Response getByCrn(@PathParam("crn") String crn);
}
