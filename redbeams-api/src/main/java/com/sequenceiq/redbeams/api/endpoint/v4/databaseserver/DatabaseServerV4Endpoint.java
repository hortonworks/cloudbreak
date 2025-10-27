package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATABASE_SERVER;
import static com.sequenceiq.cloudbreak.validation.ValidCrn.Effect.DENY;
import static com.sequenceiq.redbeams.doc.ParamDescriptions.DatabaseParamDescriptions.CRN;

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

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.doc.Notes;
import com.sequenceiq.redbeams.doc.Notes.DatabaseServerNotes;
import com.sequenceiq.redbeams.doc.OperationDescriptions;
import com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseServerOpDescription;
import com.sequenceiq.redbeams.doc.ParamDescriptions.DatabaseServerParamDescriptions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RetryAndMetrics
@Path("/v4/databaseservers")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "database servers")
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = RedbeamsApi.CRN_HEADER_API_KEY, in = SecuritySchemeIn.HEADER, paramName = "x-cdp-actor-crn")
public interface DatabaseServerV4Endpoint {

    @GET
    @Path("")
    @Operation(summary = DatabaseServerOpDescription.LIST, description = DatabaseServerNotes.LIST,
            operationId = "listDatabaseServers",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Responses list(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
            @NotNull @Parameter(description = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn
    );

    @GET
    @Path("{crn}")
    @Operation(summary = DatabaseServerOpDescription.GET_BY_CRN, description = DatabaseServerNotes.GET_BY_CRN,
            operationId = "getDatabaseServerByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response getByCrn(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn
    );

    @GET
    @Path("name/{name}")
    @Operation(summary = DatabaseServerOpDescription.GET_BY_NAME, description = DatabaseServerNotes.GET_BY_NAME,
            operationId = "getDatabaseServerByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response getByName(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
            @NotNull @Parameter(description = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn,
            @Parameter(description = DatabaseServerParamDescriptions.NAME) @PathParam("name") String name
    );

    @GET
    @Path("clusterCrn/{clusterCrn}")
    @Operation(summary = DatabaseServerOpDescription.GET_BY_CLUSTER_CRN,
            description = DatabaseServerNotes.GET_BY_CLUSTER_CRN,
            operationId = "getDatabaseServerByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response getByClusterCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
            @NotNull @Parameter(description = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn,
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT, effect = DENY)
            @NotNull @Parameter(description = DatabaseServerParamDescriptions.CLUSTER_CRN, required = true) @PathParam("clusterCrn") String clusterCrn
    );

    @GET
    @Path("clusterCrn/{clusterCrn}/list")
    @Operation(summary = DatabaseServerOpDescription.LIST_BY_CLUSTER_CRN,
            description = DatabaseServerNotes.LIST_BY_CLUSTER_CRN,
            operationId = "listDatabaseServersByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Responses listByClusterCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull @Parameter(description = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
    @QueryParam("environmentCrn") String environmentCrn,
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT, effect = DENY)
            @NotNull @Parameter(description = DatabaseServerParamDescriptions.CLUSTER_CRN, required = true) @PathParam("clusterCrn") String clusterCrn
    );

    @POST
    @Path("managed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.CREATE, description = DatabaseServerNotes.CREATE,
            operationId = "createDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerStatusV4Response create(
            @Valid @Parameter(description = DatabaseServerParamDescriptions.ALLOCATE_DATABASE_SERVER_REQUEST) AllocateDatabaseServerV4Request request
    );

    @POST
    @Path("updateclustercrn")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.UPDATE_CLUSTER_CRN,  description = DatabaseServerNotes.UPDATE_CLUSTER_CRN,
            operationId = "updateClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateClusterCrn(@QueryParam("environmentCrn") String environmentCrn, @QueryParam("currentClusterCrn") String currentClusterCrn,
            @QueryParam("newClusterCrn") String newClusterCrn, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/managed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.CREATE_INTERNAL, description = DatabaseServerNotes.CREATE,
            operationId = "createDatabaseServerInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerStatusV4Response createInternal(
            @Valid @Parameter(description = DatabaseServerParamDescriptions.ALLOCATE_DATABASE_SERVER_REQUEST) AllocateDatabaseServerV4Request request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn
    );

    @POST
    @Path("internal/managed/nonunique")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.CREATE_INTERNAL_NON_UNIQUE, description = DatabaseServerNotes.CREATE,
            operationId = "createNonUniqueDatabaseServerInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerStatusV4Response createNonUniqueInternal(
            @Valid @Parameter(description = DatabaseServerParamDescriptions.ALLOCATE_DATABASE_SERVER_REQUEST) AllocateDatabaseServerV4Request request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn
    );

    @PUT
    @Path("{crn}/release")
    @Operation(summary = DatabaseServerOpDescription.RELEASE, description = DatabaseServerNotes.RELEASE,
            operationId = "releaseManagedDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response release(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn
    );

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.REGISTER, description = DatabaseServerNotes.REGISTER,
            operationId = "registerDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response register(
            @Valid @Parameter(description = DatabaseServerParamDescriptions.DATABASE_SERVER_REQUEST) DatabaseServerV4Request request
    );

    @DELETE
    @Path("{crn}")
    @Operation(summary = DatabaseServerOpDescription.DELETE_BY_CRN, description = DatabaseServerNotes.DELETE_BY_CRN,
            operationId = "deleteDatabaseServerByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response deleteByCrn(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn,
            @QueryParam("force") @DefaultValue("false") boolean force
    );

    @DELETE
    @Path("/name/{name}")
    @Operation(summary = DatabaseServerOpDescription.DELETE_BY_NAME, description = DatabaseServerNotes.DELETE_BY_NAME,
            operationId = "deleteDatabaseServerByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Response deleteByName(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull @Parameter(description = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn,
            @Parameter(description = DatabaseServerParamDescriptions.NAME) @PathParam("name") String name,
            @QueryParam("force") @DefaultValue("false") boolean force
    );

    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.DELETE_MULTIPLE_BY_CRN, description = DatabaseServerNotes.DELETE_MULTIPLE_BY_CRN,
            operationId = "deleteMultipleDatabaseServersByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerV4Responses deleteMultiple(
            @Parameter(description = DatabaseServerParamDescriptions.CRNS) @ValidCrn(resource = DATABASE_SERVER) Set<String> crns,
            @QueryParam("force") @DefaultValue("false") boolean force
    );

    @POST
    @Path("test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.TEST_CONNECTION, description = DatabaseServerNotes.TEST_CONNECTION,
            operationId = "testDatabaseServerConnection",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerTestV4Response test(
            @Valid @Parameter(description = DatabaseServerParamDescriptions.DATABASE_SERVER_TEST_REQUEST) DatabaseServerTestV4Request request
    );

    @POST
    @Path("createDatabase")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.CREATE_DATABASE, description = DatabaseServerNotes.CREATE_DATABASE,
            operationId = "createDatabaseOnServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CreateDatabaseV4Response createDatabase(
            @Valid @Parameter(description = DatabaseServerParamDescriptions.CREATE_DATABASE_REQUEST) CreateDatabaseV4Request request
    );

    @PUT
    @Path("{crn}/start")
    @Operation(summary = DatabaseServerOpDescription.START, description = DatabaseServerNotes.START,
            operationId = "startDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void start(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn
    );

    @PUT
    @Path("internal/crn/{crn}/migrate_database_to_ssl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.MIGRATE_DATABASE_TO_SSL, description = DatabaseServerNotes.MIGRATE_DATABASE_TO_SSL,
            operationId = "migrateDatabaseToSslByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerStatusV4Response migrateDatabaseToSslByCrnInternal(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/crn/{crn}/enforce_ssl_on_database")
    @Operation(summary = DatabaseServerOpDescription.ENFORCE_SSL_ON_DATABASE, description = DatabaseServerNotes.ENFORCE_SSL_ON_DATABASE,
            operationId = "enforceSslOnDatabaseByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void enforceSslOnDatabaseByCrnInternal(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{crn}/rotate_ssl_cert")
    @Operation(summary = DatabaseServerOpDescription.ROTATE_SSL_CERT, description = DatabaseServerNotes.ROTATE_SSL_CERT,
            operationId = "rotateDatabaseServerSSLCert",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSslCert(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn
    );

    @PUT
    @Path("{crn}/update_ssl_cert")
    @Operation(summary = DatabaseServerOpDescription.UPDATE_SSL_CERT, description = DatabaseServerNotes.UPDATE_SSL_CERT,
            operationId = "updateDatabaseServerSSLCert",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateToLatestSslCert(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn
    );

    @PUT
    @Path("{crn}/stop")
    @Operation(summary = DatabaseServerOpDescription.STOP, description = DatabaseServerNotes.STOP,
            operationId = "stopDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void stop(
            @ValidCrn(resource = DATABASE_SERVER) @NotNull @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn
    );

    @PUT
    @Path("{crn}/upgrade")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.UPGRADE, description = DatabaseServerNotes.UPGRADE,
            operationId = "upgradeDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpgradeDatabaseServerV4Response upgrade(
            @ValidCrn(resource = DATABASE_SERVER) @NotEmpty @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn,
            @Valid @NotNull @Parameter(description = DatabaseServerParamDescriptions.UPGRADE_DATABASE_SERVER_REQUEST) UpgradeDatabaseServerV4Request request);

    @GET
    @Path("internal/used_subnets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.DatabaseOpDescription.GET_USED_SUBNETS_BY_ENVIRONMENT_CRN,
            description = Notes.DatabaseNotes.GET_USED_SUBNETS_BY_ENVIRONMENT_CRN, operationId = "getUsedSubnetsByEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environmentCrn") String environmentCrn);

    @PUT
    @Path("{crn}/validate_upgrade")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.VALIDATE_UPGRADE, description = DatabaseServerNotes.VALIDATE_UPGRADE,
            operationId = "validateUpgradeDatabaseServer",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpgradeDatabaseServerV4Response validateUpgrade(
            @ValidCrn(resource = DATABASE_SERVER) @NotEmpty @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn,
            @Valid @NotNull @Parameter(description = DatabaseServerParamDescriptions.VALIDATE_UPGRADE_DATABASE_SERVER_REQUEST)
            UpgradeDatabaseServerV4Request request);

    @PUT
    @Path("{crn}/validate_upgrade_cleanup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.VALIDATE_UPGRADE_CLEANUP, description = DatabaseServerNotes.VALIDATE_UPGRADE_CLEANUP,
            operationId = "validateUpgradeDatabaseServerCleanup",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpgradeDatabaseServerV4Response validateUpgradeCleanup(
            @ValidCrn(resource = DATABASE_SERVER) @NotEmpty @Parameter(description = DatabaseServerParamDescriptions.CRN)
            @PathParam("crn") String crn);

    @PUT
    @Path("internal/rotate_secret")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.ROTATE, description = DatabaseServerNotes.ROTATE,
            operationId = "rotateSecrets",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSecret(@Valid @NotNull @Parameter(description = DatabaseServerParamDescriptions.ROTATE_DATABASE_SERVER_SECRETS_REQUEST)
            RotateDatabaseServerSecretV4Request request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/sync_outdated_secrets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Sync outdated vault secrets for DB Server", operationId = "syncOutdatedSecrets",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void syncOutdatedSecrets(@ValidCrn(resource = DATABASE_SERVER) @NotEmpty @Parameter(description = CRN) @QueryParam("crn") String crn);

    @POST
    @Path("get_certificate_status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.LIST_CERTIFICATE_STATUS, description = DatabaseServerNotes.LIST_CERTIFICATE_STATUS,
            operationId = "listDatabaseServersCertificateStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            @Valid @NotNull @Parameter(description = DatabaseServerParamDescriptions.DATABASE_SERVER_CERTIFICATE_REQUEST)
            DatabaseServerCertificateStatusV4Request request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn
    );

    @POST
    @Path("internal/get_database_certificate_status_by_stack_crns")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerOpDescription.LIST_CERTIFICATE_STATUS, description = DatabaseServerNotes.LIST_CERTIFICATE_STATUS,
            operationId = "listDatabaseServersCertificateStatusByStackCrns",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatusByStackCrns(
            @Valid @NotNull @Parameter(description = DatabaseServerParamDescriptions.DATABASE_SERVER_CERTIFICATE_REQUEST)
            ClusterDatabaseServerCertificateStatusV4Request request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn
    );

    @PUT
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerNotes.RETRY, description = DatabaseServerNotes.RETRY, operationId = "retryV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier retry(
            @QueryParam("database") @ValidCrn(resource = DATABASE_SERVER) @NotNull String databaseCrn);

    @GET
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseServerNotes.LIST_RETRYABLE_FLOWS, description = DatabaseServerNotes.LIST_RETRYABLE_FLOWS, operationId = "listRetryableFlowsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<RetryableFlowResponse> listRetryableFlows(
            @QueryParam("database") @ValidCrn(resource = DATABASE_SERVER) @NotNull String databaseCrn);
}