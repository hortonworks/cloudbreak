package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRNS_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN_INSTANCES_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE_INTERNAL;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StatusCrnsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackInstancesV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/internal/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/internal/distrox", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface DistroXInternalV1Endpoint {

    @GET
    @Path("crn/{crn}")
    @ApiOperation(value = GET_BY_CRN_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXInternalV1ByCrn")
    StackViewV4Response getByCrn(@PathParam("crn") String crn);

    @GET
    @Path("crn/{crn}/instances")
    @ApiOperation(value = GET_BY_CRN_INSTANCES_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXInstancesInternalV1ByCrn")
    StackInstancesV4Responses getInstancesByCrn(@ValidCrn(resource = {CrnResourceDescriptor.DATAHUB}) @PathParam("crn") String crn);

    @POST
    @Path("crn/status")
    @ApiOperation(value = GET_BY_CRNS_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXStatusInternalV1ByCrns")
    StackStatusV4Responses getStatusByCrns(StatusCrnsV4Request request);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RENEW_CERTIFICATE_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.RENEW_CERTIFICATE_NOTES,
            nickname = "renewInternalDistroXCertificate")
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);
}
