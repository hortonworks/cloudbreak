package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_DATABASE_SERVER_BY_CLUSTER_CRN;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/v1/distrox")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DistroXDatabaseServerV1Endpoint {

    @GET
    @Path("/crn/{clusterCrn}/dbserver")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_DATABASE_SERVER_BY_CLUSTER_CRN, produces = MediaType.APPLICATION_JSON, nickname = "getDatabaseServerByClusterCrn")
    StackDatabaseServerResponse getDatabaseServerByCrn(@PathParam("clusterCrn") @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) String clusterCrn);
}
