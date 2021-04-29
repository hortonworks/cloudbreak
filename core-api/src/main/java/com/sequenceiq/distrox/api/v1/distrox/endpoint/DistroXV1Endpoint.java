package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE_BY_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE;
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
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STACK_REQUEST_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST_FLOW_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.POST_STACK_FOR_BLUEPRINT;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.REPAIR_CLUSTER_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RETRY_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SCALE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SCALE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.START_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.START_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.STOP_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.STOP_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SYNC_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SYNC_BY_NAME;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.docs.DiagnosticsOperationDescriptions;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.CmDiagnosticsCollectionV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.DiagnosticsCollectionV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RetryAndMetrics
@Path("/v1/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface DistroXV1Endpoint {

    @GET
    @Path("")
    @ApiOperation(value = LIST, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "listDistroXV1")
    StackViewV4Responses list(
            @QueryParam("environmentName") String environmentName,
            @QueryParam("environmentCrn") String environmentCrn);

    @POST
    @Path("")
    @ApiOperation(value = CREATE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "postDistroXV1")
    StackV4Response post(@Valid DistroXV1Request request);

    @POST
    @Path("internal")
    @ApiOperation(value = CREATE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "postDistroXInternalV1")
    StackV4Response postInternal(@QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn,
            @QueryParam("accountId") String accountId, @Valid DistroXV1Request request);

    @GET
    @Path("name/{name}")
    @ApiOperation(value = GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXV1ByName")
    StackV4Response getByName(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("crn/{crn}")
    @ApiOperation(value = GET_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXV1ByCrn")
    StackV4Response getByCrn(@PathParam("crn") String crn, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("name/{name}")
    @ApiOperation(value = DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteDistroXV1ByName")
    void deleteByName(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("crn/{crn}")
    @ApiOperation(value = DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteDistroXV1ByCrn")
    void deleteByCrn(@PathParam("crn") String crn, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("")
    @ApiOperation(value = DELETE_MULTIPLE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteMultipleDistroXClustersByNamesV1")
    void deleteMultiple(DistroXMultiDeleteV1Request multiDeleteRequest, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("name/{name}/sync")
    @ApiOperation(value = SYNC_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "syncDistroXV1ByName")
    void syncByName(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/sync")
    @ApiOperation(value = SYNC_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "syncDistroXV1ByCrn")
    void syncByCrn(@PathParam("crn") String crn);

    @POST
    @Path("name/{name}/retry")
    @ApiOperation(value = RETRY_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.RETRY_STACK_NOTES,
            nickname = "retryDistroXV1ByName")
    void retryByName(@PathParam("name") String name);

    @GET
    @Path("name/{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.StackOpDescription.LIST_RETRYABLE_FLOWS, produces = MediaType.APPLICATION_JSON,
            notes = Notes.LIST_RETRYABLE_NOTES, nickname = "listRetryableFlowsDistroXV1")
    List<RetryableFlowResponse> listRetryableFlows(@PathParam("name") String name);

    @POST
    @Path("crn/{crn}/retry")
    @ApiOperation(value = RETRY_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.RETRY_STACK_NOTES,
            nickname = "retryDistroXV1ByCrn")
    void retryByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/stop")
    @ApiOperation(value = STOP_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "stopDistroXV1ByName")
    FlowIdentifier putStopByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/stop")
    @ApiOperation(value = STOP_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "stopDistroXV1ByCrn")
    FlowIdentifier putStopByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("name/stop")
    @ApiOperation(value = STOP_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "stopDistroXV1ByNames")
    void putStopByNames(@QueryParam("names") List<String> names);

    @PUT
    @Path("crn/stop")
    @ApiOperation(value = STOP_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "stopDistroXV1ByCrns")
    void putStopByCrns(@QueryParam("crns") List<String> crns);

    @PUT
    @Path("name/{name}/start")
    @ApiOperation(value = START_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "startDistroXV1ByName")
    FlowIdentifier putStartByName(@PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/start")
    @ApiOperation(value = START_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "startDistroXV1ByCrn")
    FlowIdentifier putStartByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("name/start")
    @ApiOperation(value = START_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "startDistroXV1ByNames")
    void putStartByNames(@QueryParam("names") List<String> names);

    @PUT
    @Path("crn/start")
    @ApiOperation(value = START_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "startDistroXV1ByCrns")
    void putStartByCrns(@QueryParam("crns") List<String> crns);

    @PUT
    @Path("name/{name}/scaling")
    @ApiOperation(value = SCALE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "putScalingDistroXV1ByName")
    FlowIdentifier putScalingByName(@PathParam("name") String name, @Valid DistroXScaleV1Request updateRequest);

    @PUT
    @Path("crn/{crn}/scaling")
    @ApiOperation(value = SCALE_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "putScalingDistroXV1ByCrn")
    void putScalingByCrn(@PathParam("crn") String crn, @Valid DistroXScaleV1Request updateRequest);

    @POST
    @Path("name/{name}/manual_repair")
    @ApiOperation(value = REPAIR_CLUSTER_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_REPAIR_NOTES,
            nickname = "repairDistroXV1ByName")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = FlowIdentifier.class),
            @ApiResponse(code = 0, message = "unsuccessful operation", response = Void.class)
    })
    FlowIdentifier repairClusterByName(@PathParam("name") String name, @Valid DistroXRepairV1Request clusterRepairRequest);

    @POST
    @Path("crn/{crn}/manual_repair")
    @ApiOperation(value = REPAIR_CLUSTER_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_REPAIR_NOTES,
            nickname = "repairDistroXV1ByCrn")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "successful operation", response = FlowIdentifier.class),
            @ApiResponse(code = 0, message = "unsuccessful operation", response = Void.class)
    })
    FlowIdentifier repairClusterByCrn(@PathParam("crn") String crn, @Valid DistroXRepairV1Request clusterRepairRequest);

    @GET
    @Path("name/{name}/cli_create")
    @ApiOperation(value = GET_STACK_REQUEST_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXRequestV1ByName")
    Object getRequestfromName(@PathParam("name") String name);

    @GET
    @Path("crn/{crn}/cli_create")
    @ApiOperation(value = GET_STACK_REQUEST_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getDistroXRequestV1ByCrn")
    Object getRequestfromCrn(@PathParam("crn") String crn);

    @GET
    @Path("name/{name}/status")
    @ApiOperation(value = GET_STATUS_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "statusDistroXV1ByName")
    StackStatusV4Response getStatusByName(@PathParam("name") String name);

    @GET
    @Path("crn/{crn}/status")
    @ApiOperation(value = GET_STATUS_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "statusDistroXV1ByCrn")
    StackStatusV4Response getStatusByCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("name/{name}/instance")
    @ApiOperation(value = DELETE_INSTANCE_BY_ID_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstanceDistroXV1ByName")
    void deleteInstanceByName(@PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("crn/{crn}/instance")
    @ApiOperation(value = DELETE_INSTANCE_BY_ID_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstanceDistroXV1ByCrn")
    void deleteInstanceByCrn(@PathParam("crn") String crn,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("name/{name}/instances")
    @ApiOperation(value = DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstancesDistroXV1ByName")
    void deleteInstancesByName(@PathParam("name") String name,
            @QueryParam("id") List<String> instances,
            MultipleInstanceDeleteRequest request,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("crn/{crn}/instances")
    @ApiOperation(value = DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstancesDistroXV1ByCrn")
    void deleteInstancesByCrn(@PathParam("crn") String crn,
            @QueryParam("id") List<String> instances,
            MultipleInstanceDeleteRequest request,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("name/{name}/maintenance")
    @ApiOperation(value = SET_MAINTENANCE_MODE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.MAINTENANCE_NOTES,
            nickname = "setDistroXMaintenanceModeByName")
    void setClusterMaintenanceModeByName(@PathParam("name") String name,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @PUT
    @Path("crn/{crn}/maintenance")
    @ApiOperation(value = SET_MAINTENANCE_MODE_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.MAINTENANCE_NOTES,
            nickname = "setDistroXMaintenanceModeByCrn")
    void setClusterMaintenanceModeByCrn(@PathParam("crn") String crn,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode);

    @DELETE
    @Path("name/{name}/cluster")
    @ApiOperation(value = DELETE_WITH_KERBEROS_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "deleteWithKerberosDistroXV1ByName")
    void deleteWithKerberosByName(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("crn/{crn}/cluster")
    @ApiOperation(value = DELETE_WITH_KERBEROS_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "deleteWithKerberosDistroXV1ByCrn")
    void deleteWithKerberosByCrn(@PathParam("crn") String crn, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CLI_COMMAND, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "getCreateClusterForCli")
    Object getCreateAwsClusterForCli(@NotNull @Valid DistroXV1Request request);

    @POST
    @Path("blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = POST_STACK_FOR_BLUEPRINT, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "postDistroXForBlueprintV1")
    GeneratedBlueprintV4Response postStackForBlueprint(@Valid DistroXV1Request stackRequest);

    @POST
    @Path("diagnostics")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_DIAGNOSTICS, produces = MediaType.APPLICATION_JSON,
            nickname = "collectDistroxCmDiagnosticsV4")
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionV1Request request);

    @GET
    @Path("diagnostics/logs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS, produces = MediaType.APPLICATION_JSON,
            nickname = "getDistroxCmVmLogsV4")
    VmLogsResponse getVmLogs();

    @GET
    @Path("diagnostics/{crn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.LIST_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            nickname = "listDistroxDiagnosticsCollectionsV1")
    ListDiagnosticsCollectionResponse listCollections(@PathParam("crn") String crn);

    @POST
    @Path("diagnostics/{crn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.CANCEL_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            nickname = "cancelDistroxDiagnosticsCollectionsV1")
    void cancelCollections(@PathParam("crn") String crn);

    @POST
    @Path("diagnostics/cm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_CM_DIAGNOSTICS, produces = MediaType.APPLICATION_JSON,
            nickname = "collectDistroxCmBasedDiagnosticsV1")
    FlowIdentifier collectCmDiagnostics(@Valid CmDiagnosticsCollectionV1Request request);

    @GET
    @Path("diagnostics/cm/{stackCrn}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_CM_ROLES, produces = MediaType.APPLICATION_JSON,
            nickname = "getDistroxCmRoles")
    List<String> getCmRoles(@PathParam("stackCrn") String stackCrn);

    @GET
    @Path("progress/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_LAST_FLOW_PROGRESS, produces = "application/json", notes = Notes.FLOW_OPERATION_PROGRESS_NOTES,
            nickname = "getDistroXLastFlowLogProgressByResourceCrn")
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("progress/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_FLOW_PROGRESS, produces = "application/json", notes = Notes.FLOW_OPERATION_PROGRESS_NOTES,
            nickname = "getDistroXFlowLogsProgressByResourceCrn")
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RENEW_CERTIFICATE, produces = MediaType.APPLICATION_JSON, notes = Notes.RENEW_CERTIFICATE_NOTES,
            nickname = "renewDistroXCertificate")
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);

    @PUT
    @Path("name/{name}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ROTATE_CERTIFICATES, nickname = "rotateAutoTlsCertificatesByName")
    CertificatesRotationV4Response rotateAutoTlsCertificatesByName(@PathParam("name") String name,
            @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("crn/{crn}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ROTATE_CERTIFICATES, nickname = "rotateAutoTlsCertificatesByCrn")
    CertificatesRotationV4Response rotateAutoTlsCertificatesByCrn(@PathParam("crn") String crn, @Valid CertificatesRotationV4Request rotateCertificateRequest);
}
