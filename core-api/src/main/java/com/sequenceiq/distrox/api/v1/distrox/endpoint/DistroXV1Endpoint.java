package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.WorkspaceOpDescription.DELETE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CREATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_INSTANCE_BY_ID;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_WITH_KERBEROS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.POST_STACK_FOR_BLUEPRINT;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SCALE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.START_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.STOP_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SYNC_BY_NAME;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/V1/distrox", protocols = "http,https")
public interface DistroXV1Endpoint {

    @GET
    @Path("")
    @ApiOperation(value = LIST, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "listDistroXV1")
    StackViewV4Responses list(@QueryParam("environment") String environment,
            @QueryParam("onlyDatalakes") @DefaultValue("false") Boolean onlyDatalakes);

    @POST
    @Path("")
    @ApiOperation(value = CREATE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "postDistroXV1")
    StackV4Response post(@Valid DistroXV1Request request);

    @GET
    @Path("{name}")
    @ApiOperation(value = GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getDistroXV1")
    StackV4Response get(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("{name}")
    @ApiOperation(value = DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "deleteDistroXV1")
    void delete(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @PUT
    @Path("{name}/sync")
    @ApiOperation(value = SYNC_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "syncDistroXV1")
    void putSync(@PathParam("name") String name);

    @PUT
    @Path("{name}/retry")
    @ApiOperation(value = RETRY_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.RETRY_STACK_NOTES, nickname = "retryDistroXV1")
    void putRetry(@PathParam("name") String name);

    @PUT
    @Path("{name}/stop")
    @ApiOperation(value = STOP_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "stopDistroXV1")
    void putStop(@PathParam("name") String name);

    @PUT
    @Path("{name}/start")
    @ApiOperation(value = START_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "startDistroXV1")
    void putStart(@PathParam("name") String name);

    @PUT
    @Path("{name}/scaling")
    @ApiOperation(value = SCALE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "putScalingDistroXV1")
    void putScaling(@PathParam("name") String name, @Valid DistroXScaleV1Request updateRequest);

    @POST
    @Path("{name}/manual_repair")
    @ApiOperation(value = REPAIR_CLUSTER, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_REPAIR_NOTES, nickname = "repairDistroXV1")
    void repairCluster(@PathParam("name") String name, @Valid DistroXRepairV1Request clusterRepairRequest);

    @POST
    @Path("{name}/blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = POST_STACK_FOR_BLUEPRINT, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "postDistroXForBlueprintV1")
    GeneratedBlueprintV4Response postStackForBlueprint(@PathParam("name") String name, @Valid DistroXV1Request stackRequest);

    @GET
    @Path("{name}/request")
    @ApiOperation(value = GET_STACK_REQUEST, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getDistroXRequestFromNameV1")
    DistroXV1Request getRequestfromName(@PathParam("name") String name);

    @GET
    @Path("{name}/status")
    @ApiOperation(value = GET_STATUS_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "statusDistroXV1")
    StackStatusV4Response getStatusByName(@PathParam("name") String name);

    @DELETE
    @Path("{name}/instance")
    @ApiOperation(value = DELETE_INSTANCE_BY_ID, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "deleteInstanceDistroXV1")
    void deleteInstance(@PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @PUT
    @Path("{name}/maintenance")
    @ApiOperation(value = SET_MAINTENANCE_MODE, produces = MediaType.APPLICATION_JSON, notes = Notes.MAINTENANCE_NOTES, nickname = "setDistroXMaintenanceMode")
    void setClusterMaintenanceMode(@PathParam("name") String name,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @DELETE
    @Path("{name}/cluster")
    @ApiOperation(value = DELETE_WITH_KERBEROS, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_NOTES, nickname = "deleteWithKerberosDistroXV1")
    void deleteWithKerberos(@PathParam("name") String name);
}
