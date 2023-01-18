package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import static com.sequenceiq.common.api.util.versionchecker.VersionCheckerModelDescription.CHECK_CLIENT_VERSION;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RenewCertificateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DeploymentPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ResourceEventResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.AccountPreferencesDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RepositoryConfigsValidationOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SecurityRuleOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.util.UtilControllerDescription;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/utils")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/utils", description = UtilControllerDescription.UTIL_DESCRIPTION)
public interface UtilV4Endpoint {

    @GET
    @Path("client")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_CLIENT_VERSION, operationId = "checkClientVersionV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    VersionCheckResult checkClientVersion(@QueryParam("version") String version);

    @GET
    @Path("stack_matrix")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.STACK_MATRIX, operationId = "getStackMatrixUtilV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackMatrixV4Response getStackMatrix(
        @QueryParam("imageCatalogName") String imageCatalogName,
        @QueryParam("platform") String platform,
        @QueryParam("govCloud") boolean govCloud,
        @QueryParam("os") String os) throws Exception;

    @GET
    @Path("cloud_storage_matrix")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.CLOUD_STORAGE_MATRIX, operationId = "getCloudStorageMatrixV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = "Define stack version at least at patch level eg. 2.6.0")
    CloudStorageSupportedV4Responses getCloudStorageMatrix(@QueryParam("stackVersion") String stackVersion);

    @POST
    @Path("validate_repository")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RepositoryConfigsValidationOpDescription.POST_REPOSITORY_CONFIGS_VALIDATION,
            description = Notes.REPOSITORY_CONFIGS_VALIDATION_NOTES, operationId = "repositoryConfigsValidationV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RepoConfigValidationV4Response repositoryConfigValidationRequest(@Valid RepoConfigValidationV4Request repoConfigValidationV4Request);

    @GET
    @Path("default_security_rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SecurityRuleOpDescription.GET_DEFAULT_SECURITY_RULES,
            description = Notes.SECURITY_RULE_NOTES, operationId = "getDefaultSecurityRules",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SecurityRulesV4Response getDefaultSecurityRules();

    @GET
    @Path("deployment")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountPreferencesDescription.GET, description = Notes.ACCOUNT_PREFERENCES_NOTES,
            operationId = "getDeploymentInfo",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DeploymentPreferencesV4Response deployment();

    @POST
    @Path("notification_test")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.NOTIFICATION_TEST, description = Notes.ACCOUNT_PREFERENCES_NOTES,
            operationId = "postNotificationTest",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ResourceEventResponse postNotificationTest();

    @POST
    @Path("renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.RENEW_CERTIFICATE, description = Notes.RENEW_CERTIFICATE_NOTES,
            operationId = "renewCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response renewCertificate(RenewCertificateV4Request renewCertificateV4Request);

    @GET
    @Path("used_images")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.USED_IMAGES, operationId = "usedImages",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UsedImagesListV4Response usedImages(@QueryParam("thresholdInDays") Integer thresholdInDays);
}
