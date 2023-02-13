package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.UPDATE_SALT;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ATTACH_RECIPE_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ATTACH_RECIPE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHANGE_IMAGE_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DETACH_RECIPE_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DETACH_RECIPE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GENERATE_IMAGE_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_RECIPES_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_RECIPES_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ROTATE_CERTIFICATES;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CLI_COMMAND;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CREATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_INSTANCE_BY_ID_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_INSTANCE_BY_ID_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_MULTIPLE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_WITH_KERBEROS_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_WITH_KERBEROS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_LAST_FLOW_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_OPERATION;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST_FLOW_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.MODIFY_PROXY_CONFIG_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.POST_STACK_FOR_BLUEPRINT;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RESTART_CLUSTER_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ROTATE_SALT_PASSWORD_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SCALE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SCALE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.START_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.START_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.STOP_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.STOP_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SYNC_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SYNC_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.VERTICAL_SCALE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.VERTICAL_SCALE_BY_NAME;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.DistroXSyncCmV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXGenerateImageCatalogV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXVerticalScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.docs.DiagnosticsOperationDescriptions;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.CmDiagnosticsCollectionV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.DiagnosticsCollectionV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox")
public interface DistroXV1Endpoint {

    @GET
    @Path("")
    @Operation(summary =  LIST, description =  Notes.STACK_NOTES,
            operationId = "listDistroXV1")
    StackViewV4Responses list(
            @QueryParam("environmentName") String environmentName,
            @QueryParam("environmentCrn") String environmentCrn);

    @POST
    @Path("")
    @Operation(summary =  CREATE, description =  Notes.STACK_NOTES,
            operationId = "postDistroXV1")
    StackV4Response post(@Valid DistroXV1Request request);

    @POST
    @Path("internal")
    @Operation(summary =  CREATE, description =  Notes.STACK_NOTES,
            operationId = "postDistroXInternalV1")
    StackV4Response postInternal(@QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn,
            @QueryParam("accountId") String accountId, @Valid DistroXV1Request request);

    @GET
    @Path("name/{name}")
    @Operation(summary =  GET_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "getDistroXV1ByName")
    StackV4Response getByName(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("crn/{crn}")
    @Operation(summary =  GET_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "getDistroXV1ByCrn")
    StackV4Response getByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("name/{name}")
    @Operation(summary =  DELETE_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "deleteDistroXV1ByName")
    void deleteByName(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("crn/{crn}")
    @Operation(summary =  DELETE_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "deleteDistroXV1ByCrn")
    void deleteByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("")
    @Operation(summary =  DELETE_MULTIPLE, description =  Notes.STACK_NOTES,
            operationId = "deleteMultipleDistroXClustersByNamesV1")
    void deleteMultiple(DistroXMultiDeleteV1Request multiDeleteRequest, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("name/{name}/sync")
    @Operation(summary =  SYNC_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "syncDistroXV1ByName")
    void syncByName(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/sync")
    @Operation(summary =  SYNC_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "syncDistroXV1ByCrn")
    void syncByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @POST
    @Path("name/{name}/retry")
    @Operation(summary =  RETRY_BY_NAME, description =  Notes.RETRY_STACK_NOTES,
            operationId = "retryDistroXV1ByName")
    void retryByName(@PathParam("name") String name);

    @GET
    @Path("name/{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  OperationDescriptions.StackOpDescription.LIST_RETRYABLE_FLOWS,
            description =  Notes.LIST_RETRYABLE_NOTES, operationId ="listRetryableFlowsDistroXV1")
    List<RetryableFlowResponse> listRetryableFlows(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/retry")
    @Operation(summary =  RETRY_BY_CRN, description =  Notes.RETRY_STACK_NOTES,
            operationId = "retryDistroXV1ByCrn")
    void retryByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/stop")
    @Operation(summary =  STOP_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByName")
    FlowIdentifier putStopByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/stop")
    @Operation(summary =  STOP_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByCrn")
    FlowIdentifier putStopByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/stop")
    @Operation(summary =  STOP_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByNames")
    void putStopByNames(@QueryParam("names") List<String> names);

    @PUT
    @Path("crn/stop")
    @Operation(summary =  STOP_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByCrns")
    void putStopByCrns(@QueryParam("crns") List<String> crns);

    @PUT
    @Path("name/{name}/start")
    @Operation(summary =  START_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "startDistroXV1ByName")
    FlowIdentifier putStartByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/start")
    @Operation(summary =  START_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "startDistroXV1ByCrn")
    FlowIdentifier putStartByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/start")
    @Operation(summary =  START_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "startDistroXV1ByNames")
    void putStartByNames(@QueryParam("names") List<String> names);

    @PUT
    @Path("crn/start")
    @Operation(summary =  START_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "startDistroXV1ByCrns")
    void putStartByCrns(@QueryParam("crns") List<String> crns);

    @PUT
    @Path("crn/restartCluster")
    @Operation(summary =  RESTART_CLUSTER_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "restartDistroXClusterByCrns")
    void restartClusterServicesByCrns(@QueryParam("crns") List<String> crns);

    @POST
    @Path("crn/{crn}/rotate_salt_password")
    @Operation(summary =  ROTATE_SALT_PASSWORD_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "rotateSaltPasswordDistroXV1ByCrn")
    FlowIdentifier rotateSaltPasswordByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/salt_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  UPDATE_SALT, operationId ="updateSaltDistroxV1ByCrn")
    FlowIdentifier updateSaltByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/scaling")
    @Operation(summary =  SCALE_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "putScalingDistroXV1ByName")
    FlowIdentifier putScalingByName(@PathParam("name") String name, @Valid DistroXScaleV1Request updateRequest);

    @PUT
    @Path("crn/{crn}/scaling")
    @Operation(summary =  SCALE_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "putScalingDistroXV1ByCrn")
    void putScalingByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn, @Valid DistroXScaleV1Request updateRequest);

    @PUT
    @Path("name/{name}/vertical_scaling")
    @Operation(summary =  VERTICAL_SCALE_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "verticalScalingByName")
    FlowIdentifier verticalScalingByName(
            @PathParam("name") String name,
            @Valid DistroXVerticalScaleV1Request updateRequest);

    @PUT
    @Path("crn/{crn}/vertical_scaling")
    @Operation(summary =  VERTICAL_SCALE_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "verticalScalingByCrn")
    FlowIdentifier verticalScalingByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXVerticalScaleV1Request updateRequest);

    @POST
    @Path("name/{name}/manual_repair")
    @Operation(summary = REPAIR_CLUSTER_BY_NAME, description =  Notes.CLUSTER_REPAIR_NOTES,
            operationId = "repairDistroXV1ByName")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = {
                    @Content(schema = @Schema(implementation =  FlowIdentifier.class)) }),
            @ApiResponse(responseCode = "0", description = "unsuccessful operation", content = {
                    @Content(schema = @Schema()) })
    })
    FlowIdentifier repairClusterByName(@PathParam("name") String name, @Valid DistroXRepairV1Request clusterRepairRequest);

    @POST
    @Path("crn/{crn}/manual_repair")
    @Operation(summary =  REPAIR_CLUSTER_BY_CRN, description =  Notes.CLUSTER_REPAIR_NOTES,
            operationId = "repairDistroXV1ByCrn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = {
                    @Content(schema = @Schema(implementation =  FlowIdentifier.class)) }),
            @ApiResponse(responseCode = "0", description = "unsuccessful operation", content = {
                    @Content(schema = @Schema()) })
    })
    FlowIdentifier repairClusterByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXRepairV1Request clusterRepairRequest);

    @GET
    @Path("name/{name}/cli_create")
    @Operation(summary =  GET_STACK_REQUEST_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "getDistroXRequestV1ByName")
    Object getRequestfromName(@PathParam("name") String name);

    @GET
    @Path("crn/{crn}/cli_create")
    @Operation(summary =  GET_STACK_REQUEST_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "getDistroXRequestV1ByCrn")
    Object getRequestfromCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @GET
    @Path("name/{name}/status")
    @Operation(summary =  GET_STATUS_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "statusDistroXV1ByName")
    StackStatusV4Response getStatusByName(@PathParam("name") String name);

    @GET
    @Path("crn/{crn}/status")
    @Operation(summary =  GET_STATUS_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "statusDistroXV1ByCrn")
    StackStatusV4Response getStatusByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @DELETE
    @Path("name/{name}/instance")
    @Operation(summary =  DELETE_INSTANCE_BY_ID_BY_NAME, description =  Notes.STACK_NOTES,
            operationId = "deleteInstanceDistroXV1ByName")
    FlowIdentifier deleteInstanceByName(@PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("crn/{crn}/instance")
    @Operation(summary =  DELETE_INSTANCE_BY_ID_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "deleteInstanceDistroXV1ByCrn")
    FlowIdentifier deleteInstanceByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("name/{name}/instances")
    @Operation(summary =  DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, description =  Notes.STACK_NOTES,
            operationId = "deleteInstancesDistroXV1ByName")
    FlowIdentifier deleteInstancesByName(@PathParam("name") String name,
            @QueryParam("id") List<String> instances,
            MultipleInstanceDeleteRequest request,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("crn/{crn}/instances")
    @Operation(summary =  DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, description =  Notes.STACK_NOTES,
            operationId = "deleteInstancesDistroXV1ByCrn")
    FlowIdentifier deleteInstancesByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @QueryParam("id") List<String> instances,
            MultipleInstanceDeleteRequest request,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("name/{name}/maintenance")
    @Operation(summary =  SET_MAINTENANCE_MODE_BY_NAME, description =  Notes.MAINTENANCE_NOTES,
            operationId = "setDistroXMaintenanceModeByName")
    void setClusterMaintenanceModeByName(@PathParam("name") String name,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @PUT
    @Path("crn/{crn}/maintenance")
    @Operation(summary =  SET_MAINTENANCE_MODE_BY_CRN, description =  Notes.MAINTENANCE_NOTES,
            operationId = "setDistroXMaintenanceModeByCrn")
    void setClusterMaintenanceModeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @DELETE
    @Path("name/{name}/cluster")
    @Operation(summary =  DELETE_WITH_KERBEROS_BY_NAME, description =  Notes.CLUSTER_NOTES,
            operationId = "deleteWithKerberosDistroXV1ByName")
    void deleteWithKerberosByName(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("crn/{crn}/cluster")
    @Operation(summary =  DELETE_WITH_KERBEROS_BY_CRN, description =  Notes.CLUSTER_NOTES,
            operationId = "deleteWithKerberosDistroXV1ByCrn")
    void deleteWithKerberosByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  CLI_COMMAND, description =  Notes.CLUSTER_NOTES,
            operationId = "getCreateClusterForCli")
    Object getCreateAwsClusterForCli(@NotNull @Valid DistroXV1Request request);

    @POST
    @Path("blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  POST_STACK_FOR_BLUEPRINT, description =  Notes.STACK_NOTES,
            operationId = "postDistroXForBlueprintV1")
    GeneratedBlueprintV4Response postStackForBlueprint(@Valid DistroXV1Request stackRequest);

    @POST
    @Path("diagnostics")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DiagnosticsOperationDescriptions.COLLECT_DIAGNOSTICS,
            operationId = "collectDistroxCmDiagnosticsV4")
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionV1Request request);

    @GET
    @Path("diagnostics/logs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS,
            operationId = "getDistroxCmVmLogsV4")
    VmLogsResponse getVmLogs();

    @GET
    @Path("diagnostics/{crn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DiagnosticsOperationDescriptions.LIST_COLLECTIONS,
            operationId = "listDistroxDiagnosticsCollectionsV1")
    ListDiagnosticsCollectionResponse listCollections(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @POST
    @Path("diagnostics/{crn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DiagnosticsOperationDescriptions.CANCEL_COLLECTIONS,
            operationId = "cancelDistroxDiagnosticsCollectionsV1")
    void cancelCollections(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @POST
    @Path("diagnostics/cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DiagnosticsOperationDescriptions.COLLECT_CM_DIAGNOSTICS,
            operationId = "collectDistroxCmBasedDiagnosticsV1")
    FlowIdentifier collectCmDiagnostics(@Valid CmDiagnosticsCollectionV1Request request);

    @GET
    @Path("diagnostics/cm/{stackCrn}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DiagnosticsOperationDescriptions.GET_CM_ROLES,
            operationId = "getDistroxCmRoles")
    List<String> getCmRoles(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("stackCrn") String stackCrn);

    @GET
    @Path("progress/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  GET_LAST_FLOW_PROGRESS, description =  Notes.FLOW_OPERATION_PROGRESS_NOTES,
            operationId = "getDistroXLastFlowLogProgressByResourceCrn")
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("progress/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  LIST_FLOW_PROGRESS, description =  Notes.FLOW_OPERATION_PROGRESS_NOTES,
            operationId = "getDistroXFlowLogsProgressByResourceCrn")
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("operation/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  GET_OPERATION, description =  Notes.FLOW_OPERATION_PROGRESS_NOTES,
            operationId = "getDistroXOperationProgressByResourceCrn")
    OperationView getOperationProgressByResourceCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  RENEW_CERTIFICATE, description =  Notes.RENEW_CERTIFICATE_NOTES,
            operationId = "renewDistroXCertificate")
    FlowIdentifier renewCertificate(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ROTATE_CERTIFICATES, operationId ="rotateAutoTlsCertificatesByName")
    CertificatesRotationV4Response rotateAutoTlsCertificatesByName(@PathParam("name") String name,
            @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("crn/{crn}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ROTATE_CERTIFICATES, operationId ="rotateAutoTlsCertificatesByCrn")
    CertificatesRotationV4Response rotateAutoTlsCertificatesByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("name/{name}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  REFRESH_RECIPES_BY_NAME, operationId ="refreshRecipesByName")
    UpdateRecipesV4Response refreshRecipesByName(@PathParam("name") String name, @Valid UpdateRecipesV4Request request);

    @PUT
    @Path("crn/{crn}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  REFRESH_RECIPES_BY_CRN, operationId ="refreshRecipesByCrn")
    UpdateRecipesV4Response refreshRecipesByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid UpdateRecipesV4Request request);

    @POST
    @Path("crn/{crn}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ATTACH_RECIPE_BY_CRN, operationId ="attachRecipesByCrn")
    AttachRecipeV4Response attachRecipeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid AttachRecipeV4Request request);

    @POST
    @Path("name/{name}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ATTACH_RECIPE_BY_NAME, operationId ="attachRecipesByName")
    AttachRecipeV4Response attachRecipeByName(@PathParam("name") String name, @Valid AttachRecipeV4Request request);

    @POST
    @Path("crn/{crn}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DETACH_RECIPE_BY_CRN, operationId ="detachRecipesByCrn")
    DetachRecipeV4Response detachRecipeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid DetachRecipeV4Request request);

    @POST
    @Path("name/{name}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  DETACH_RECIPE_BY_NAME, operationId ="detachRecipesByName")
    DetachRecipeV4Response detachRecipeByName(@PathParam("name") String name, @Valid DetachRecipeV4Request request);

    @POST
    @Path("{name}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "syncs from distrox cluster CM the CM and parcel versions", operationId ="syncDistroxCm")
    DistroXSyncCmV1Response syncComponentVersionsFromCmByName(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "syncs from distrox cluster CM the CM and parcel versions", operationId ="syncDistroxCmByCrn")
    DistroXSyncCmV1Response syncComponentVersionsFromCmByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  CHANGE_IMAGE_CATALOG, operationId ="changeImageCatalog")
    void changeImageCatalog(@PathParam("name") String name, @Valid @NotNull ChangeImageCatalogV4Request changeImageCatalogRequest);

    @GET
    @Path("name/{name}/generate_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  GENERATE_IMAGE_CATALOG, operationId ="generateImageCatalog")
    DistroXGenerateImageCatalogV1Response generateImageCatalog(@PathParam("name") String name);

    @GET
    @Path("{name}/get_cluster_recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "validates if the distrox cluster is recoverable or not", operationId ="getClusterRecoverableByName")
    RecoveryValidationV4Response getClusterRecoverableByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/get_cluster_recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "validates if the distrox cluster is recoverable or not", operationId ="getClusterRecoverableByCrn")
    RecoveryValidationV4Response getClusterRecoverableByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/modify_proxy")
    @ApiOperation(value = MODIFY_PROXY_CONFIG_INTERNAL, produces = MediaType.APPLICATION_JSON, notes = Notes.MODIFY_PROXY_CONFIG_NOTES,
            nickname = "modifyProxyConfigDistroXInternalV1ByCrn")
    FlowIdentifier modifyProxyInternal(@ValidCrn(resource = {CrnResourceDescriptor.DATAHUB}) @PathParam("crn") String crn,
            @ValidCrn(resource = {CrnResourceDescriptor.PROXY}) @QueryParam("previousProxyConfigCrn") String previousProxyConfigCrn,
            String initiatorUserCrn);
}
