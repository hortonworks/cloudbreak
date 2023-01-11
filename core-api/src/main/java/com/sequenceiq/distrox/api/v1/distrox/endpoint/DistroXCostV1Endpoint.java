package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/distrox/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox/cost", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface DistroXCostV1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DistroXOpDescription.COST, produces = MediaType.APPLICATION_JSON,
            notes = Notes.CLUSTER_COST_NOTES, nickname = "listDistroXCostV1")
    RealTimeCostResponse list(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) List<String> datahubCrns);
}
