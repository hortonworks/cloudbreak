package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATAHUB;
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
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ADD_VOLUMES_BY_STACK_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ADD_VOLUMES_BY_STACK_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CLI_COMMAND;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CREATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_INSTANCE_BY_ID_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_INSTANCE_BY_ID_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_MULTIPLE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_VOLUMES_BY_STACK_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_VOLUMES_BY_STACK_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_WITH_KERBEROS_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DELETE_WITH_KERBEROS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_ENDPOINTS_BY_CRNS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_LAST_FLOW_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_OPERATION;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.IMD_UPDATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST_BY_SERVICE_TYPES;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST_FLOW_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.MIGRATE_DATABASE_TO_SSL_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.MIGRATE_DATABASE_TO_SSL_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.MODIFY_PROXY_CONFIG_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.POST_STACK_FOR_BLUEPRINT;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RDS_CERTIFICATE_ROTATION_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RDS_CERTIFICATE_ROTATION_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RESTART_CLUSTER_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ROOT_VOLUME_UPDATE_BY_DH_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ROOT_VOLUME_UPDATE_BY_DH_NAME;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.DatahubDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.DistroXSyncCmV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackEndpointV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.migraterds.MigrateDatabaseV1Response;
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
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXGenerateImageCatalogV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXInstanceMetadataUpdateV1Request;
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
import com.sequenceiq.distrox.api.v1.distrox.model.rotaterdscert.RotateRdsCertificateV1Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox")
public interface DistroXV1Endpoint {

    @GET
    @Path("")
    @Operation(summary = LIST, description = Notes.STACK_NOTES,
            operationId = "listDistroXV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackViewV4Responses list(
            @QueryParam("environmentName") String environmentName,
            @QueryParam("environmentCrn") String environmentCrn);

    @GET
    @Path("list_by_service_types")
    @Operation(summary = LIST_BY_SERVICE_TYPES, description = Notes.STACK_NOTES,
            operationId = "listByServiceTypesDistroXV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackViewV4Responses listByServiceTypes(@QueryParam("serviceTypes") List<String> serviceTypes);

    @POST
    @Path("")
    @Operation(summary = CREATE, description = Notes.STACK_NOTES,
            operationId = "postDistroXV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response post(@Valid DistroXV1Request request);

    @POST
    @Path("internal")
    @Operation(summary = CREATE, description = Notes.STACK_NOTES,
            operationId = "postDistroXInternalV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response postInternal(@QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn,
            @QueryParam("accountId") String accountId, @Valid DistroXV1Request request);

    @GET
    @Path("name/{name}")
    @Operation(summary = GET_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "getDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response getByName(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("crn/{crn}")
    @Operation(summary = GET_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "getDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response getByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("name/{name}")
    @Operation(summary = DELETE_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "deleteDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteByName(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("crn/{crn}")
    @Operation(summary = DELETE_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "deleteDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("")
    @Operation(summary = DELETE_MULTIPLE, description = Notes.STACK_NOTES,
            operationId = "deleteMultipleDistroXClustersByNamesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteMultiple(DistroXMultiDeleteV1Request multiDeleteRequest, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("name/{name}/sync")
    @Operation(summary = SYNC_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "syncDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier syncByName(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/sync")
    @Operation(summary = SYNC_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "syncDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier syncByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @POST
    @Path("name/{name}/retry")
    @Operation(summary = RETRY_BY_NAME, description = Notes.RETRY_STACK_NOTES,
            operationId = "retryDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier retryByName(@PathParam("name") String name);

    @GET
    @Path("name/{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.StackOpDescription.LIST_RETRYABLE_FLOWS,
            description = Notes.LIST_RETRYABLE_NOTES, operationId = "listRetryableFlowsDistroXV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<RetryableFlowResponse> listRetryableFlows(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/retry")
    @Operation(summary = RETRY_BY_CRN, description = Notes.RETRY_STACK_NOTES,
            operationId = "retryDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier retryByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/stop")
    @Operation(summary = STOP_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStopByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/stop")
    @Operation(summary = STOP_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStopByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/stop")
    @Operation(summary = STOP_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByNames",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void putStopByNames(@QueryParam("names") List<String> names);

    @PUT
    @Path("crn/stop")
    @Operation(summary = STOP_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "stopDistroXV1ByCrns",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void putStopByCrns(@QueryParam("crns") List<String> crns);

    @PUT
    @Path("name/{name}/start")
    @Operation(summary = START_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "startDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStartByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/start")
    @Operation(summary = START_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "startDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStartByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/start")
    @Operation(summary = START_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "startDistroXV1ByNames",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void putStartByNames(@QueryParam("names") List<String> names);

    @PUT
    @Path("crn/start")
    @Operation(summary = START_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "startDistroXV1ByCrns",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void putStartByCrns(@QueryParam("crns") List<String> crns);

    @PUT
    @Path("crn/restartCluster")
    @Operation(summary = RESTART_CLUSTER_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "restartDistroXClusterByCrns",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowIdentifier> restartClusterServicesByCrns(@QueryParam("crns") List<String> crns,
            @QueryParam("refreshRemoteDataContext") @DefaultValue("false") Boolean refreshRemoteDataContext);

    /**
     * @deprecated use rotate_secret endpoint with secret type SALT_PASSWORD instead
     */
    @Deprecated
    @POST
    @Path("crn/{crn}/rotate_salt_password")
    @Operation(summary = ROTATE_SALT_PASSWORD_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "rotateSaltPasswordDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSaltPasswordByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/salt_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_SALT, operationId = "updateSaltDistroxV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateSaltByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/scaling")
    @Operation(summary = SCALE_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "putScalingDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putScalingByName(@PathParam("name") String name, @Valid DistroXScaleV1Request updateRequest);

    @PUT
    @Path("crn/{crn}/scaling")
    @Operation(summary = SCALE_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "putScalingDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void putScalingByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn, @Valid DistroXScaleV1Request updateRequest);

    @PUT
    @Path("name/{name}/vertical_scaling")
    @Operation(summary = VERTICAL_SCALE_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "verticalScalingByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier verticalScalingByName(
            @PathParam("name") String name,
            @Valid DistroXVerticalScaleV1Request updateRequest);

    @PUT
    @Path("crn/{crn}/vertical_scaling")
    @Operation(summary = VERTICAL_SCALE_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "verticalScalingByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier verticalScalingByCrn(
            @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXVerticalScaleV1Request updateRequest);

    @POST
    @Path("name/{name}/manual_repair")
    @Operation(summary = REPAIR_CLUSTER_BY_NAME, description = Notes.CLUSTER_REPAIR_NOTES,
            operationId = "repairDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier repairClusterByName(@PathParam("name") String name, @Valid DistroXRepairV1Request clusterRepairRequest);

    @POST
    @Path("crn/{crn}/manual_repair")
    @Operation(summary = REPAIR_CLUSTER_BY_CRN, description = Notes.CLUSTER_REPAIR_NOTES,
            operationId = "repairDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier repairClusterByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid DistroXRepairV1Request clusterRepairRequest);

    @PUT
    @Path("name/{name}/rotate_rds_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RDS_CERTIFICATE_ROTATION_BY_NAME, description = Notes.ROTATE_RDS_CERTIFICATE_NOTES,
            operationId = "rotateRdsCertificateByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RotateRdsCertificateV1Response rotateRdsCertificateByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/rotate_rds_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RDS_CERTIFICATE_ROTATION_BY_CRN, description = Notes.ROTATE_RDS_CERTIFICATE_NOTES,
            operationId = "rotateRdsCertificateByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RotateRdsCertificateV1Response rotateRdsCertificateByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/migrate_database_to_ssl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MIGRATE_DATABASE_TO_SSL_BY_NAME, description = Notes.MIGRATE_DATABASE_TO_SSL_NOTES,
            operationId = "migrateDatabaseToSslByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    MigrateDatabaseV1Response migrateDatabaseToSslByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/migrate_database_to_ssl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MIGRATE_DATABASE_TO_SSL_BY_CRN, description = Notes.MIGRATE_DATABASE_TO_SSL_NOTES,
            operationId = "migrateDatabaseToSslByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    MigrateDatabaseV1Response migrateDatabaseToSslByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @GET
    @Path("name/{name}/cli_create")
    @Operation(summary = GET_STACK_REQUEST_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "getDistroXRequestV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getRequestfromName(@PathParam("name") String name);

    @GET
    @Path("crn/{crn}/cli_create")
    @Operation(summary = GET_STACK_REQUEST_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "getDistroXRequestV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getRequestfromCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @GET
    @Path("name/{name}/status")
    @Operation(summary = GET_STATUS_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "statusDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackStatusV4Response getStatusByName(@PathParam("name") String name);

    @GET
    @Path("crn/{crn}/status")
    @Operation(summary = GET_STATUS_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "statusDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackStatusV4Response getStatusByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @GET
    @Path("crn/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_ENDPOINTS_BY_CRNS, description = Notes.STACK_NOTES,
            operationId = "endpointsDistroXV1ByCrns",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackEndpointV4Responses getEndpointsByCrns(@QueryParam("crns") List<String> crns);

    @DELETE
    @Path("name/{name}/instance")
    @Operation(summary = DELETE_INSTANCE_BY_ID_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "deleteInstanceDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteInstanceByName(@PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("crn/{crn}/instance")
    @Operation(summary = DELETE_INSTANCE_BY_ID_BY_CRN, description = Notes.STACK_NOTES,
            operationId = "deleteInstanceDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteInstanceByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("name/{name}/instances")
    @Operation(summary = DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "deleteInstancesDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteInstancesByName(@PathParam("name") String name,
            @QueryParam("id") List<String> instances,
            MultipleInstanceDeleteRequest request,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("crn/{crn}/instances")
    @Operation(summary = DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "deleteInstancesDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteInstancesByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @QueryParam("id") List<String> instances,
            MultipleInstanceDeleteRequest request,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("name/{name}/maintenance")
    @Operation(summary = SET_MAINTENANCE_MODE_BY_NAME, description = Notes.MAINTENANCE_NOTES,
            operationId = "setDistroXMaintenanceModeByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void setClusterMaintenanceModeByName(@PathParam("name") String name,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @PUT
    @Path("crn/{crn}/maintenance")
    @Operation(summary = SET_MAINTENANCE_MODE_BY_CRN, description = Notes.MAINTENANCE_NOTES,
            operationId = "setDistroXMaintenanceModeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void setClusterMaintenanceModeByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @DELETE
    @Path("name/{name}/cluster")
    @Operation(summary = DELETE_WITH_KERBEROS_BY_NAME, description = Notes.CLUSTER_NOTES,
            operationId = "deleteWithKerberosDistroXV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteWithKerberosByName(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("crn/{crn}/cluster")
    @Operation(summary = DELETE_WITH_KERBEROS_BY_CRN, description = Notes.CLUSTER_NOTES,
            operationId = "deleteWithKerberosDistroXV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteWithKerberosByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CLI_COMMAND, description = Notes.CLUSTER_NOTES,
            operationId = "getCreateClusterForCli",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getCreateAwsClusterForCli(@NotNull @Valid DistroXV1Request request);

    @POST
    @Path("blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = POST_STACK_FOR_BLUEPRINT, description = Notes.STACK_NOTES,
            operationId = "postDistroXForBlueprintV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    GeneratedBlueprintV4Response postStackForBlueprint(@Valid DistroXV1Request stackRequest);

    @POST
    @Path("diagnostics")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.COLLECT_DIAGNOSTICS,
            operationId = "collectDistroxCmDiagnosticsV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionV1Request request);

    @GET
    @Path("diagnostics/logs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS,
            operationId = "getDistroxCmVmLogsV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    VmLogsResponse getVmLogs();

    @GET
    @Path("diagnostics/{crn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.LIST_COLLECTIONS,
            operationId = "listDistroxDiagnosticsCollectionsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ListDiagnosticsCollectionResponse listCollections(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @POST
    @Path("diagnostics/{crn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.CANCEL_COLLECTIONS,
            operationId = "cancelDistroxDiagnosticsCollectionsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void cancelCollections(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @POST
    @Path("diagnostics/cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.COLLECT_CM_DIAGNOSTICS,
            operationId = "collectDistroxCmBasedDiagnosticsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier collectCmDiagnostics(@Valid CmDiagnosticsCollectionV1Request request);

    @GET
    @Path("diagnostics/cm/{stackCrn}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.GET_CM_ROLES,
            operationId = "getDistroxCmRoles",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<String> getCmRoles(@ValidCrn(resource = DATAHUB) @PathParam("stackCrn") String stackCrn);

    @GET
    @Path("progress/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_LAST_FLOW_PROGRESS, description = Notes.FLOW_OPERATION_PROGRESS_NOTES,
            operationId = "getDistroXLastFlowLogProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ValidCrn(resource = DATAHUB) @PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("progress/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_FLOW_PROGRESS, description = Notes.FLOW_OPERATION_PROGRESS_NOTES,
            operationId = "getDistroXFlowLogsProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(
            @ValidCrn(resource = DATAHUB) @PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("operation/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_OPERATION, description = Notes.FLOW_OPERATION_PROGRESS_NOTES,
            operationId = "getDistroXOperationProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationView getOperationProgressByResourceCrn(@ValidCrn(resource = DATAHUB) @PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RENEW_CERTIFICATE, description = Notes.RENEW_CERTIFICATE_NOTES,
            operationId = "renewDistroXCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier renewCertificate(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_CERTIFICATES, operationId = "rotateAutoTlsCertificatesByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CertificatesRotationV4Response rotateAutoTlsCertificatesByName(@PathParam("name") String name,
            @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("crn/{crn}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_CERTIFICATES, operationId = "rotateAutoTlsCertificatesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CertificatesRotationV4Response rotateAutoTlsCertificatesByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("name/{name}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REFRESH_RECIPES_BY_NAME, operationId = "refreshRecipesByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpdateRecipesV4Response refreshRecipesByName(@PathParam("name") String name, @Valid UpdateRecipesV4Request request);

    @PUT
    @Path("crn/{crn}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REFRESH_RECIPES_BY_CRN, operationId = "refreshRecipesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpdateRecipesV4Response refreshRecipesByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid UpdateRecipesV4Request request);

    @POST
    @Path("crn/{crn}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ATTACH_RECIPE_BY_CRN, operationId = "attachRecipesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AttachRecipeV4Response attachRecipeByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid AttachRecipeV4Request request);

    @POST
    @Path("name/{name}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ATTACH_RECIPE_BY_NAME, operationId = "attachRecipesByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AttachRecipeV4Response attachRecipeByName(@PathParam("name") String name, @Valid AttachRecipeV4Request request);

    @POST
    @Path("crn/{crn}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DETACH_RECIPE_BY_CRN, operationId = "detachRecipesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetachRecipeV4Response detachRecipeByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid DetachRecipeV4Request request);

    @POST
    @Path("name/{name}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DETACH_RECIPE_BY_NAME, operationId = "detachRecipesByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetachRecipeV4Response detachRecipeByName(@PathParam("name") String name, @Valid DetachRecipeV4Request request);

    @POST
    @Path("{name}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "syncs from distrox cluster CM the CM and parcel versions", operationId = "syncDistroxCm",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXSyncCmV1Response syncComponentVersionsFromCmByName(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "syncs from distrox cluster CM the CM and parcel versions", operationId = "syncDistroxCmByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXSyncCmV1Response syncComponentVersionsFromCmByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHANGE_IMAGE_CATALOG, operationId = "changeImageCatalog",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void changeImageCatalog(@PathParam("name") String name, @Valid @NotNull ChangeImageCatalogV4Request changeImageCatalogRequest);

    @GET
    @Path("name/{name}/generate_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GENERATE_IMAGE_CATALOG, operationId = "generateImageCatalog",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DistroXGenerateImageCatalogV1Response generateImageCatalog(@PathParam("name") String name);

    @GET
    @Path("{name}/get_cluster_recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validates if the distrox cluster is recoverable or not", operationId = "getClusterRecoverableByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecoveryValidationV4Response getClusterRecoverableByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/get_cluster_recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validates if the distrox cluster is recoverable or not", operationId = "getClusterRecoverableByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecoveryValidationV4Response getClusterRecoverableByCrn(@ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/modify_proxy")
    @Operation(summary = MODIFY_PROXY_CONFIG_INTERNAL, description = Notes.MODIFY_PROXY_CONFIG_NOTES,
            operationId = "modifyProxyConfigDistroXInternalV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier modifyProxyInternal(@ValidCrn(resource = {DATAHUB}) @PathParam("crn") String crn,
            @ValidCrn(resource = {CrnResourceDescriptor.PROXY}) @QueryParam("previousProxyConfigCrn") String previousProxyConfigCrn,
            String initiatorUserCrn);

    @PUT
    @Path("name/{name}/delete_volumes")
    @Operation(summary = DELETE_VOLUMES_BY_STACK_NAME, description = Notes.STACK_NOTES,
            operationId = "deleteVolumesByStackName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteVolumesByStackName(
            @PathParam("name") String name,
            @Valid StackDeleteVolumesRequest deleteRequest);

    @PUT
    @Path("crn/{crn}/delete_volumes")
    @Operation(summary = DELETE_VOLUMES_BY_STACK_CRN, description = Notes.STACK_NOTES,
            operationId = "deleteVolumesByStackCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteVolumesByStackCrn(
            @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid StackDeleteVolumesRequest deleteRequest);

    @PUT
    @Path("/name/{name}/disk_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Updates disk type and resizes DH", operationId = "diskUpdateByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier diskUpdateByName(@PathParam("name") String name, @Valid DiskUpdateRequest updateRequest);

    @PUT
    @Path("/crn/{crn}/disk_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Updates disk type and resizes DH", operationId = "diskUpdateByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier diskUpdateByCrn(
            @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn, @Valid DiskUpdateRequest updateRequest);

    @PUT
    @Path("name/{name}/add_volumes")
    @Operation(summary = ADD_VOLUMES_BY_STACK_NAME, operationId = "addVolumesByStackName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier addVolumesByStackName(
            @PathParam("name") String name,
            @Valid StackAddVolumesRequest addVolumesRequest);

    @PUT
    @Path("crn/{crn}/add_volumes")
    @Operation(summary = ADD_VOLUMES_BY_STACK_CRN, operationId = "addVolumesByStackCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier addVolumesByStackCrn(
            @ValidCrn(resource = {CrnResourceDescriptor.VM_DATALAKE, DATAHUB}) @PathParam("crn") String crn,
            @Valid StackAddVolumesRequest addVolumesRequest);

    @PUT
    @Path("imd_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = IMD_UPDATE, operationId = "imdUpdate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier instanceMetadataUpdate(@Valid @NotNull DistroXInstanceMetadataUpdateV1Request request);

    @POST
    @Path("get_database_certificate_status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Lists all database servers certificate status that are known, either because they were " +
            "registered or because this service created them.",
            operationId = "listDataHubDatabaseServersCertificateStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            @Valid @NotNull DatahubDatabaseServerCertificateStatusV4Request request);

    @PUT
    @Path("name/{name}/modify_root_volume")
    @Operation(summary = ROOT_VOLUME_UPDATE_BY_DH_NAME, operationId = "updateRootVolumeByDatahubName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateRootVolumeByDatahubName(
            @PathParam("name") String name,
            @Valid DiskUpdateRequest rootDiskVolumesRequest) throws Exception;

    @PUT
    @Path("crn/{crn}/modify_root_volume")
    @Operation(summary = ROOT_VOLUME_UPDATE_BY_DH_CRN, operationId = "updateRootVolumeByDatahubCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateRootVolumeByDatahubCrn(
            @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @Valid DiskUpdateRequest rootDiskVolumesRequest) throws Exception;

    @POST
    @Path("name/{name}/set_default_java_version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set default java version on the DataHub by its name",
            operationId = "setDataHubDefaultJavaVersionByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier setDefaultJavaVersionByName(@PathParam("name") String name, @NotNull @Valid SetDefaultJavaVersionRequest request);

    @POST
    @Path("crn/{crn}/set_default_java_version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set default java version on the DataHub by its CRN",
            operationId = "setDataHubDefaultJavaVersionByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier setDefaultJavaVersionByCrn(@NotEmpty @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @NotNull @Valid SetDefaultJavaVersionRequest request);

    @GET
    @Path("crn/{crn}/list_available_java_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List available java versions on the DataHub by its CRN",
            operationId = "listDataHubAvailableJavaVersionsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<String> listAvailableJavaVersionsByCrn(@NotEmpty @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn);

    @PUT
    @Path("/name/{name}/modify_selinux/{selinuxMode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Modifies SELinux on a specific DH", operationId = "modifySelinuxByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier modifySeLinuxByName(@PathParam("name") String name, @PathParam("selinuxMode") SeLinux selinuxMode);

    @PUT
    @Path("/crn/{crn}/modify_selinux/{selinuxMode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Modifies SELinux on a specific DH", operationId = "modifySelinuxByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier modifySeLinuxByCrn(@NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.VM_DATALAKE, DATAHUB}) @PathParam("crn") String crn,
            @PathParam("selinuxMode") SeLinux selinuxMode);

    @PUT
    @Path("name/{name}/trigger_sku_migration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Triggering SKU migration on the DataHub by its name", operationId = "triggerDataHubSkuMigrationByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier triggerSkuMigrationByName(@PathParam("name") String name,
            @QueryParam("force") @DefaultValue("false") boolean force);

    @PUT
    @Path("crn/{crn}/trigger_sku_migration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Triggering SKU migration on the DataHub by its crn", operationId = "triggerDataHubSkuMigrationByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier triggerSkuMigrationByCrn(@NotEmpty @ValidCrn(resource = DATAHUB) @PathParam("crn") String crn,
            @QueryParam("force") @DefaultValue("false") boolean force);

    @PUT
    @Path("crn/{crn}/migrate_from_zookeeper_to_kraft")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiate the migration from Zookeeper to KRaft broker in Kafka.", operationId = "migrateFromZookeeperToKraftByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier migrateFromZookeeperToKraftByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);
}
