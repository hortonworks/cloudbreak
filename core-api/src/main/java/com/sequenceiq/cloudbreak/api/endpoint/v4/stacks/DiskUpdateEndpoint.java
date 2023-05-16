package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskModificationRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/disks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "v4/disks", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DiskUpdateEndpoint {

    @GET
    @Path("/{platform}/diskTypeSupported")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checks if Disk Type Change is supported", produces = MediaType.APPLICATION_JSON, nickname = "checksPlatformPermission")
    Boolean isDiskTypeChangeSupported(@NotEmpty @PathParam("platform") String platform);

    @PUT
    @Path("/updateDisks")
    @ApiOperation(value = "Updates Disk Type and/ size", produces = MediaType.APPLICATION_JSON, nickname = "updateDiskTypeAndSize")
    void updateDiskTypeAndSize(@Valid DiskModificationRequest diskModificationRequest) throws Exception;

    @PUT
    @Path("/{stackId}/stopCluster")
    @ApiOperation(value = "Stops CM services running on Datalake", produces = MediaType.APPLICATION_JSON, nickname = "stopCluster")
    void stopCMServices(@PathParam("stackId") long stackId) throws Exception;

    @PUT
    @Path("/{stackId}/startCluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Start CM services running on Datalake", produces = MediaType.APPLICATION_JSON, nickname = "startCluster")
    void startCMServices(@PathParam("stackId") long stackId) throws Exception;

    @PUT
    @Path("/{stackId}/resizeDisks/{instanceGroup}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Resizes disks using Orchestrator", produces = MediaType.APPLICATION_JSON, nickname = "resizeDisks")
    FlowIdentifier resizeDisks(@PathParam("stackId") long stackId, @PathParam("instanceGroup") String instanceGroup) throws Exception;
}
