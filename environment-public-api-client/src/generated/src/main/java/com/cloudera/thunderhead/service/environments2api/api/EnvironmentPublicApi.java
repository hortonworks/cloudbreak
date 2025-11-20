package com.cloudera.thunderhead.service.environments2api.api;

import com.cloudera.thunderhead.service.environments2api.ApiException;
import com.cloudera.thunderhead.service.environments2api.ApiClient;
import com.cloudera.thunderhead.service.environments2api.ApiResponse;
import com.cloudera.thunderhead.service.environments2api.Configuration;
import com.cloudera.thunderhead.service.environments2api.Pair;

import jakarta.ws.rs.core.GenericType;

import com.cloudera.thunderhead.service.environments2api.model.AttachFreeIpaRecipesRequest;
import com.cloudera.thunderhead.service.environments2api.model.CancelFreeipaDiagnosticsRequest;
import com.cloudera.thunderhead.service.environments2api.model.CancelTrustRequest;
import com.cloudera.thunderhead.service.environments2api.model.CancelTrustResponse;
import com.cloudera.thunderhead.service.environments2api.model.ChangeEnvironmentCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.ChangeEnvironmentCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.CheckDatabaseConnectivityRequest;
import com.cloudera.thunderhead.service.environments2api.model.CheckDatabaseConnectivityResponse;
import com.cloudera.thunderhead.service.environments2api.model.CheckEnvironmentConnectivityRequest;
import com.cloudera.thunderhead.service.environments2api.model.CheckEnvironmentConnectivityResponse;
import com.cloudera.thunderhead.service.environments2api.model.CheckKubernetesConnectivityRequest;
import com.cloudera.thunderhead.service.environments2api.model.CheckKubernetesConnectivityResponse;
import com.cloudera.thunderhead.service.environments2api.model.CollectFreeipaDiagnosticsRequest;
import com.cloudera.thunderhead.service.environments2api.model.CollectFreeipaDiagnosticsResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSGovCloudCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSGovCloudCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSGovCloudEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateAWSGovCloudEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateAzureCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateAzureCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateAzureEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateAzureEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateEncryptionProfileRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateEncryptionProfileResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateGCPCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateGCPCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateGCPEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateGCPEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreatePrivateEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreatePrivateEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateProxyConfigRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateProxyConfigResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateYARNCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateYARNCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.CreateYARNEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.CreateYARNEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.DeleteAuditCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.DeleteCaCertificateFromTruststoreRequest;
import com.cloudera.thunderhead.service.environments2api.model.DeleteCaCertificateFromTruststoreResponse;
import com.cloudera.thunderhead.service.environments2api.model.DeleteCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.DeleteEncryptionProfileRequest;
import com.cloudera.thunderhead.service.environments2api.model.DeleteEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.DeleteIdBrokerMappingsRequest;
import com.cloudera.thunderhead.service.environments2api.model.DeleteProxyConfigRequest;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.DetachFreeIpaRecipesRequest;
import com.cloudera.thunderhead.service.environments2api.model.DisableS3GuardRequest;
import com.cloudera.thunderhead.service.environments2api.model.DisableS3GuardResponse;
import com.cloudera.thunderhead.service.environments2api.model.DownscaleFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.DownscaleFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.Error;
import com.cloudera.thunderhead.service.environments2api.model.ExtractCatalogRequest;
import com.cloudera.thunderhead.service.environments2api.model.ExtractCatalogResponse;
import com.cloudera.thunderhead.service.environments2api.model.FinishSetupTrustRequest;
import com.cloudera.thunderhead.service.environments2api.model.FinishSetupTrustResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetAccountTelemetryDefaultResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetAccountTelemetryResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetAuditCredentialPrerequisitesRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetAuditCredentialPrerequisitesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetAutomatedSyncEnvironmentStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetAutomatedSyncEnvironmentStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetAzureImageTermsPolicyResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetCliForCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetCliForCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetCliForEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetCliForEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetConfigFilesRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetConfigFilesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetCredentialPrerequisitesRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetCredentialPrerequisitesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetEnvironmentSettingRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetEnvironmentSettingResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetEnvironmentUserSyncStateRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetEnvironmentUserSyncStateResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaLogDescriptorsResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaSecretEncryptionKeyIdsRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaSecretEncryptionKeyIdsResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaUpgradeOptionsRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetFreeipaUpgradeOptionsResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetGovCloudAuditCredentialPrerequisitesRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetGovCloudAuditCredentialPrerequisitesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetGovCloudCredentialPrerequisitesRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetGovCloudCredentialPrerequisitesResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetIdBrokerMappingsCliForEnvRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetIdBrokerMappingsCliForEnvResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetIdBrokerMappingsRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetIdBrokerMappingsResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetIdBrokerMappingsSyncStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetIdBrokerMappingsSyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetKeytabRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetKeytabResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetOperationRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetOperationResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRepairFreeipaStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRepairFreeipaStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.GetTruststorePasswordRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetTruststorePasswordResponse;
import com.cloudera.thunderhead.service.environments2api.model.InitializeAWSComputeClusterRequest;
import com.cloudera.thunderhead.service.environments2api.model.InitializeAWSComputeClusterResponse;
import com.cloudera.thunderhead.service.environments2api.model.InitializeAzureComputeClusterRequest;
import com.cloudera.thunderhead.service.environments2api.model.InitializeAzureComputeClusterResponse;
import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListAuditCredentialsResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListConnectedDataServicesRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListConnectedDataServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListCredentialsRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListCredentialsResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListEncryptionProfilesRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListEncryptionProfilesResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListFreeipaDiagnosticsRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListFreeipaDiagnosticsResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListFreeipaSecretTypesRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListFreeipaSecretTypesResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListProxyConfigsRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListProxyConfigsResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListTruststoreCertificatesRequest;
import com.cloudera.thunderhead.service.environments2api.model.ListTruststoreCertificatesResponse;
import com.cloudera.thunderhead.service.environments2api.model.ModifySelinuxRequest;
import com.cloudera.thunderhead.service.environments2api.model.ModifySelinuxResponse;
import com.cloudera.thunderhead.service.environments2api.model.RebuildFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.RebuildFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.RepairFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.RepairFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.RetryFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.RetryFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.RotateFreeipaSecretsRequest;
import com.cloudera.thunderhead.service.environments2api.model.RotateFreeipaSecretsResponse;
import com.cloudera.thunderhead.service.environments2api.model.RotateSaltPasswordRequest;
import com.cloudera.thunderhead.service.environments2api.model.RotateSaltPasswordResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetAWSAuditCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetAWSAuditCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetAWSGovCloudAuditCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetAWSGovCloudAuditCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetAccountTelemetryRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetAccountTelemetryResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetAzureAuditCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetAzureAuditCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetCatalogRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetEndpointAccessGatewayRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetEndpointAccessGatewayResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetEnvironmentSettingRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetGCPAuditCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetGCPAuditCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetIdBrokerMappingsRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetIdBrokerMappingsResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetPasswordRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetPasswordResponse;
import com.cloudera.thunderhead.service.environments2api.model.SetTelemetryFeaturesRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetupTrustRequest;
import com.cloudera.thunderhead.service.environments2api.model.SetupTrustResponse;
import com.cloudera.thunderhead.service.environments2api.model.StartEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.StartEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.StartFreeIpaVerticalScalingRequest;
import com.cloudera.thunderhead.service.environments2api.model.StartFreeIpaVerticalScalingResponse;
import com.cloudera.thunderhead.service.environments2api.model.StopEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.StopEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncAllUsersRequest;
import com.cloudera.thunderhead.service.environments2api.model.SyncAllUsersResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncIdBrokerMappingsRequest;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncUserResponse;
import com.cloudera.thunderhead.service.environments2api.model.TestAccountTelemetryRulesRequest;
import com.cloudera.thunderhead.service.environments2api.model.TestAccountTelemetryRulesResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAwsCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAwsCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAwsDiskEncryptionParametersRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAwsDiskEncryptionParametersResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureAvailabilityZonesRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureAvailabilityZonesResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureDatabaseResourcesRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureDatabaseResourcesResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureEncryptionResourcesRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureEncryptionResourcesResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateAzureImageTermsPolicyRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateCustomDockerRegistryRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateCustomDockerRegistryResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateDataServiceResourcesRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateDataServiceResourcesResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateFreeipaToAwsImdsV1Request;
import com.cloudera.thunderhead.service.environments2api.model.UpdateFreeipaToAwsImdsV1Response;
import com.cloudera.thunderhead.service.environments2api.model.UpdateFreeipaToAwsImdsV2Request;
import com.cloudera.thunderhead.service.environments2api.model.UpdateFreeipaToAwsImdsV2Response;
import com.cloudera.thunderhead.service.environments2api.model.UpdateGCPCredentialRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateGCPCredentialResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateGcpAvailabilityZonesRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateGcpAvailabilityZonesResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateOrchestratorStateRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateOrchestratorStateResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateProxyConfigRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateRootVolumeFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateRootVolumeFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateSecurityAccessRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateSecurityAccessResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateSshKeyRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateSshKeyResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateSubnetRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateSubnetResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpdateTagsRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpdateTagsResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpgradeCcmRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpgradeCcmResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpgradeFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpgradeFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.UpscaleFreeipaRequest;
import com.cloudera.thunderhead.service.environments2api.model.UpscaleFreeipaResponse;
import com.cloudera.thunderhead.service.environments2api.model.ValidateAwsCloudStorageRequest;
import com.cloudera.thunderhead.service.environments2api.model.ValidateAwsCloudStorageResponse;
import com.cloudera.thunderhead.service.environments2api.model.ValidateAzureCloudStorageRequest;
import com.cloudera.thunderhead.service.environments2api.model.ValidateAzureCloudStorageResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class EnvironmentPublicApi {
  private ApiClient apiClient;

  public EnvironmentPublicApi() {
    this(Configuration.getDefaultApiClient());
  }

  public EnvironmentPublicApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Get the API client
   *
   * @return API client
   */
  public ApiClient getApiClient() {
    return apiClient;
  }

  /**
   * Set the API client
   *
   * @param apiClient an instance of API client
   */
  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Attach recipes to FreeIPA.
   * Attach recipes to FreeIPA.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object attachFreeIpaRecipes(@jakarta.annotation.Nonnull AttachFreeIpaRecipesRequest input) throws ApiException {
    return attachFreeIpaRecipesWithHttpInfo(input).getData();
  }

  /**
   * Attach recipes to FreeIPA.
   * Attach recipes to FreeIPA.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> attachFreeIpaRecipesWithHttpInfo(@jakarta.annotation.Nonnull AttachFreeIpaRecipesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling attachFreeIpaRecipes");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.attachFreeIpaRecipes", "/api/v1/environments2/attachFreeIpaRecipes", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Cancel running FreeIPA diagnostics collections
   * Cancel running FreeIPA diagnostics collection
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object cancelFreeipaDiagnostics(@jakarta.annotation.Nonnull CancelFreeipaDiagnosticsRequest input) throws ApiException {
    return cancelFreeipaDiagnosticsWithHttpInfo(input).getData();
  }

  /**
   * Cancel running FreeIPA diagnostics collections
   * Cancel running FreeIPA diagnostics collection
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> cancelFreeipaDiagnosticsWithHttpInfo(@jakarta.annotation.Nonnull CancelFreeipaDiagnosticsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling cancelFreeipaDiagnostics");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.cancelFreeipaDiagnostics", "/api/v1/environments2/cancelFreeipaDiagnostics", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Cancel cross-realm trust for FreeIPA.
   * Cancel cross-realm trust setup between FreeIPA and Active Directory.
   * @param input  (required)
   * @return CancelTrustResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CancelTrustResponse cancelTrust(@jakarta.annotation.Nonnull CancelTrustRequest input) throws ApiException {
    return cancelTrustWithHttpInfo(input).getData();
  }

  /**
   * Cancel cross-realm trust for FreeIPA.
   * Cancel cross-realm trust setup between FreeIPA and Active Directory.
   * @param input  (required)
   * @return ApiResponse&lt;CancelTrustResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CancelTrustResponse> cancelTrustWithHttpInfo(@jakarta.annotation.Nonnull CancelTrustRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling cancelTrust");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CancelTrustResponse> localVarReturnType = new GenericType<CancelTrustResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.cancelTrust", "/api/v1/environments2/cancelTrust", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Changes the credential for an environment.
   * Changes the credential for an environment.
   * @param input  (required)
   * @return ChangeEnvironmentCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ChangeEnvironmentCredentialResponse changeEnvironmentCredential(@jakarta.annotation.Nonnull ChangeEnvironmentCredentialRequest input) throws ApiException {
    return changeEnvironmentCredentialWithHttpInfo(input).getData();
  }

  /**
   * Changes the credential for an environment.
   * Changes the credential for an environment.
   * @param input  (required)
   * @return ApiResponse&lt;ChangeEnvironmentCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ChangeEnvironmentCredentialResponse> changeEnvironmentCredentialWithHttpInfo(@jakarta.annotation.Nonnull ChangeEnvironmentCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling changeEnvironmentCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ChangeEnvironmentCredentialResponse> localVarReturnType = new GenericType<ChangeEnvironmentCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.changeEnvironmentCredential", "/api/v1/environments2/changeEnvironmentCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Checks Database connectivity based on the input parameters.
   * Checks Database connectivity based on the input parameters.
   * @param input  (required)
   * @return CheckDatabaseConnectivityResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CheckDatabaseConnectivityResponse checkDatabaseConnectivity(@jakarta.annotation.Nonnull CheckDatabaseConnectivityRequest input) throws ApiException {
    return checkDatabaseConnectivityWithHttpInfo(input).getData();
  }

  /**
   * Checks Database connectivity based on the input parameters.
   * Checks Database connectivity based on the input parameters.
   * @param input  (required)
   * @return ApiResponse&lt;CheckDatabaseConnectivityResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CheckDatabaseConnectivityResponse> checkDatabaseConnectivityWithHttpInfo(@jakarta.annotation.Nonnull CheckDatabaseConnectivityRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling checkDatabaseConnectivity");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CheckDatabaseConnectivityResponse> localVarReturnType = new GenericType<CheckDatabaseConnectivityResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.checkDatabaseConnectivity", "/api/v1/environments2/checkDatabaseConnectivity", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Checks connectivity to a new Private Cloud environment by trying to access the Cloudera Manager address with provided credentials.
   * Checks connectivity to a Private Cloud environment.
   * @param input  (required)
   * @return CheckEnvironmentConnectivityResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CheckEnvironmentConnectivityResponse checkEnvironmentConnectivity(@jakarta.annotation.Nonnull CheckEnvironmentConnectivityRequest input) throws ApiException {
    return checkEnvironmentConnectivityWithHttpInfo(input).getData();
  }

  /**
   * Checks connectivity to a new Private Cloud environment by trying to access the Cloudera Manager address with provided credentials.
   * Checks connectivity to a Private Cloud environment.
   * @param input  (required)
   * @return ApiResponse&lt;CheckEnvironmentConnectivityResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CheckEnvironmentConnectivityResponse> checkEnvironmentConnectivityWithHttpInfo(@jakarta.annotation.Nonnull CheckEnvironmentConnectivityRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling checkEnvironmentConnectivity");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CheckEnvironmentConnectivityResponse> localVarReturnType = new GenericType<CheckEnvironmentConnectivityResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.checkEnvironmentConnectivity", "/api/v1/environments2/checkEnvironmentConnectivity", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Checks connectivity to a Kubernetes address with provided kubeconfig files.
   * Checks connectivity to a Kubernetes address with provided kubeconfig files.
   * @param input  (required)
   * @return CheckKubernetesConnectivityResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CheckKubernetesConnectivityResponse checkKubernetesConnectivity(@jakarta.annotation.Nonnull CheckKubernetesConnectivityRequest input) throws ApiException {
    return checkKubernetesConnectivityWithHttpInfo(input).getData();
  }

  /**
   * Checks connectivity to a Kubernetes address with provided kubeconfig files.
   * Checks connectivity to a Kubernetes address with provided kubeconfig files.
   * @param input  (required)
   * @return ApiResponse&lt;CheckKubernetesConnectivityResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CheckKubernetesConnectivityResponse> checkKubernetesConnectivityWithHttpInfo(@jakarta.annotation.Nonnull CheckKubernetesConnectivityRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling checkKubernetesConnectivity");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CheckKubernetesConnectivityResponse> localVarReturnType = new GenericType<CheckKubernetesConnectivityResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.checkKubernetesConnectivity", "/api/v1/environments2/checkKubernetesConnectivity", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Start FreeIPA diagnostics collection
   * Start FreeIPA diagnostics collection
   * @param input  (required)
   * @return CollectFreeipaDiagnosticsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CollectFreeipaDiagnosticsResponse collectFreeipaDiagnostics(@jakarta.annotation.Nonnull CollectFreeipaDiagnosticsRequest input) throws ApiException {
    return collectFreeipaDiagnosticsWithHttpInfo(input).getData();
  }

  /**
   * Start FreeIPA diagnostics collection
   * Start FreeIPA diagnostics collection
   * @param input  (required)
   * @return ApiResponse&lt;CollectFreeipaDiagnosticsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CollectFreeipaDiagnosticsResponse> collectFreeipaDiagnosticsWithHttpInfo(@jakarta.annotation.Nonnull CollectFreeipaDiagnosticsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling collectFreeipaDiagnostics");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CollectFreeipaDiagnosticsResponse> localVarReturnType = new GenericType<CollectFreeipaDiagnosticsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.collectFreeipaDiagnostics", "/api/v1/environments2/collectFreeipaDiagnostics", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new AWS credential that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new AWS credential.
   * @param input  (required)
   * @return CreateAWSCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateAWSCredentialResponse createAWSCredential(@jakarta.annotation.Nonnull CreateAWSCredentialRequest input) throws ApiException {
    return createAWSCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates a new AWS credential that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new AWS credential.
   * @param input  (required)
   * @return ApiResponse&lt;CreateAWSCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateAWSCredentialResponse> createAWSCredentialWithHttpInfo(@jakarta.annotation.Nonnull CreateAWSCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createAWSCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateAWSCredentialResponse> localVarReturnType = new GenericType<CreateAWSCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createAWSCredential", "/api/v1/environments2/createAWSCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new AWS environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new AWS environment.
   * @param input  (required)
   * @return CreateAWSEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateAWSEnvironmentResponse createAWSEnvironment(@jakarta.annotation.Nonnull CreateAWSEnvironmentRequest input) throws ApiException {
    return createAWSEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Creates a new AWS environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new AWS environment.
   * @param input  (required)
   * @return ApiResponse&lt;CreateAWSEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateAWSEnvironmentResponse> createAWSEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull CreateAWSEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createAWSEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateAWSEnvironmentResponse> localVarReturnType = new GenericType<CreateAWSEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createAWSEnvironment", "/api/v1/environments2/createAWSEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new AWS credential for GovCloud that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new AWS credential for GovCloud.
   * @param input  (required)
   * @return CreateAWSGovCloudCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateAWSGovCloudCredentialResponse createAWSGovCloudCredential(@jakarta.annotation.Nonnull CreateAWSGovCloudCredentialRequest input) throws ApiException {
    return createAWSGovCloudCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates a new AWS credential for GovCloud that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new AWS credential for GovCloud.
   * @param input  (required)
   * @return ApiResponse&lt;CreateAWSGovCloudCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateAWSGovCloudCredentialResponse> createAWSGovCloudCredentialWithHttpInfo(@jakarta.annotation.Nonnull CreateAWSGovCloudCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createAWSGovCloudCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateAWSGovCloudCredentialResponse> localVarReturnType = new GenericType<CreateAWSGovCloudCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createAWSGovCloudCredential", "/api/v1/environments2/createAWSGovCloudCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new AWS GovCloud environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new AWS GovCloud environment.
   * @param input  (required)
   * @return CreateAWSGovCloudEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateAWSGovCloudEnvironmentResponse createAWSGovCloudEnvironment(@jakarta.annotation.Nonnull CreateAWSGovCloudEnvironmentRequest input) throws ApiException {
    return createAWSGovCloudEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Creates a new AWS GovCloud environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new AWS GovCloud environment.
   * @param input  (required)
   * @return ApiResponse&lt;CreateAWSGovCloudEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateAWSGovCloudEnvironmentResponse> createAWSGovCloudEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull CreateAWSGovCloudEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createAWSGovCloudEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateAWSGovCloudEnvironmentResponse> localVarReturnType = new GenericType<CreateAWSGovCloudEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createAWSGovCloudEnvironment", "/api/v1/environments2/createAWSGovCloudEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new Azure credential that can be attached to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new Azure credential.
   * @param input  (required)
   * @return CreateAzureCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateAzureCredentialResponse createAzureCredential(@jakarta.annotation.Nonnull CreateAzureCredentialRequest input) throws ApiException {
    return createAzureCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates a new Azure credential that can be attached to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new Azure credential.
   * @param input  (required)
   * @return ApiResponse&lt;CreateAzureCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateAzureCredentialResponse> createAzureCredentialWithHttpInfo(@jakarta.annotation.Nonnull CreateAzureCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createAzureCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateAzureCredentialResponse> localVarReturnType = new GenericType<CreateAzureCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createAzureCredential", "/api/v1/environments2/createAzureCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new Azure environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new Azure environment.
   * @param input  (required)
   * @return CreateAzureEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateAzureEnvironmentResponse createAzureEnvironment(@jakarta.annotation.Nonnull CreateAzureEnvironmentRequest input) throws ApiException {
    return createAzureEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Creates a new Azure environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new Azure environment.
   * @param input  (required)
   * @return ApiResponse&lt;CreateAzureEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateAzureEnvironmentResponse> createAzureEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull CreateAzureEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createAzureEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateAzureEnvironmentResponse> localVarReturnType = new GenericType<CreateAzureEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createAzureEnvironment", "/api/v1/environments2/createAzureEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates an encryption profile.
   * Creates an encryption profile.
   * @param input  (required)
   * @return CreateEncryptionProfileResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateEncryptionProfileResponse createEncryptionProfile(@jakarta.annotation.Nonnull CreateEncryptionProfileRequest input) throws ApiException {
    return createEncryptionProfileWithHttpInfo(input).getData();
  }

  /**
   * Creates an encryption profile.
   * Creates an encryption profile.
   * @param input  (required)
   * @return ApiResponse&lt;CreateEncryptionProfileResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateEncryptionProfileResponse> createEncryptionProfileWithHttpInfo(@jakarta.annotation.Nonnull CreateEncryptionProfileRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createEncryptionProfile");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateEncryptionProfileResponse> localVarReturnType = new GenericType<CreateEncryptionProfileResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createEncryptionProfile", "/api/v1/environments2/createEncryptionProfile", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new GCP credential that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new GCP credential.
   * @param input  (required)
   * @return CreateGCPCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateGCPCredentialResponse createGCPCredential(@jakarta.annotation.Nonnull CreateGCPCredentialRequest input) throws ApiException {
    return createGCPCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates a new GCP credential that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new GCP credential.
   * @param input  (required)
   * @return ApiResponse&lt;CreateGCPCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateGCPCredentialResponse> createGCPCredentialWithHttpInfo(@jakarta.annotation.Nonnull CreateGCPCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createGCPCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateGCPCredentialResponse> localVarReturnType = new GenericType<CreateGCPCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createGCPCredential", "/api/v1/environments2/createGCPCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new GCP environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new GCP environment.
   * @param input  (required)
   * @return CreateGCPEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateGCPEnvironmentResponse createGCPEnvironment(@jakarta.annotation.Nonnull CreateGCPEnvironmentRequest input) throws ApiException {
    return createGCPEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Creates a new GCP environment by providing the cloud provider access and network information. A FreeIPA server will be automatically provisioned when an environment is created.
   * Creates a new GCP environment.
   * @param input  (required)
   * @return ApiResponse&lt;CreateGCPEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateGCPEnvironmentResponse> createGCPEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull CreateGCPEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createGCPEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateGCPEnvironmentResponse> localVarReturnType = new GenericType<CreateGCPEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createGCPEnvironment", "/api/v1/environments2/createGCPEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new Private Cloud environment by providing the Cloudera Manager address and credentials.
   * Creates a new Private Cloud environment.
   * @param input  (required)
   * @return CreatePrivateEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreatePrivateEnvironmentResponse createPrivateEnvironment(@jakarta.annotation.Nonnull CreatePrivateEnvironmentRequest input) throws ApiException {
    return createPrivateEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Creates a new Private Cloud environment by providing the Cloudera Manager address and credentials.
   * Creates a new Private Cloud environment.
   * @param input  (required)
   * @return ApiResponse&lt;CreatePrivateEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreatePrivateEnvironmentResponse> createPrivateEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull CreatePrivateEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createPrivateEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreatePrivateEnvironmentResponse> localVarReturnType = new GenericType<CreatePrivateEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createPrivateEnvironment", "/api/v1/environments2/createPrivateEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new proxy config that can be attatched to an environment.
   * Creates a new proxy config.
   * @param input  (required)
   * @return CreateProxyConfigResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateProxyConfigResponse createProxyConfig(@jakarta.annotation.Nonnull CreateProxyConfigRequest input) throws ApiException {
    return createProxyConfigWithHttpInfo(input).getData();
  }

  /**
   * Creates a new proxy config that can be attatched to an environment.
   * Creates a new proxy config.
   * @param input  (required)
   * @return ApiResponse&lt;CreateProxyConfigResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateProxyConfigResponse> createProxyConfigWithHttpInfo(@jakarta.annotation.Nonnull CreateProxyConfigRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createProxyConfig");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateProxyConfigResponse> localVarReturnType = new GenericType<CreateProxyConfigResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createProxyConfig", "/api/v1/environments2/createProxyConfig", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new YCLOUD credential that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new YCLOUD credential.
   * @param input  (required)
   * @return CreateYARNCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateYARNCredentialResponse createYARNCredential(@jakarta.annotation.Nonnull CreateYARNCredentialRequest input) throws ApiException {
    return createYARNCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates a new YCLOUD credential that can be attatched to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Creates a new YCLOUD credential.
   * @param input  (required)
   * @return ApiResponse&lt;CreateYARNCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateYARNCredentialResponse> createYARNCredentialWithHttpInfo(@jakarta.annotation.Nonnull CreateYARNCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createYARNCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateYARNCredentialResponse> localVarReturnType = new GenericType<CreateYARNCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createYARNCredential", "/api/v1/environments2/createYARNCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates a new YCLOUD environment by providing the cloud provider access and network information.
   * Creates a new YCLOUD environment.
   * @param input  (required)
   * @return CreateYARNEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public CreateYARNEnvironmentResponse createYARNEnvironment(@jakarta.annotation.Nonnull CreateYARNEnvironmentRequest input) throws ApiException {
    return createYARNEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Creates a new YCLOUD environment by providing the cloud provider access and network information.
   * Creates a new YCLOUD environment.
   * @param input  (required)
   * @return ApiResponse&lt;CreateYARNEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<CreateYARNEnvironmentResponse> createYARNEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull CreateYARNEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling createYARNEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<CreateYARNEnvironmentResponse> localVarReturnType = new GenericType<CreateYARNEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.createYARNEnvironment", "/api/v1/environments2/createYARNEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes an audit credential.
   * Deletes an audit credential.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object deleteAuditCredential(@jakarta.annotation.Nonnull DeleteAuditCredentialRequest input) throws ApiException {
    return deleteAuditCredentialWithHttpInfo(input).getData();
  }

  /**
   * Deletes an audit credential.
   * Deletes an audit credential.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> deleteAuditCredentialWithHttpInfo(@jakarta.annotation.Nonnull DeleteAuditCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteAuditCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteAuditCredential", "/api/v1/environments2/deleteAuditCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes a CA certificate from the trust store.
   * Deletes a CA certificate from the trust store.
   * @param input  (required)
   * @return DeleteCaCertificateFromTruststoreResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public DeleteCaCertificateFromTruststoreResponse deleteCaCertificateFromTruststore(@jakarta.annotation.Nonnull DeleteCaCertificateFromTruststoreRequest input) throws ApiException {
    return deleteCaCertificateFromTruststoreWithHttpInfo(input).getData();
  }

  /**
   * Deletes a CA certificate from the trust store.
   * Deletes a CA certificate from the trust store.
   * @param input  (required)
   * @return ApiResponse&lt;DeleteCaCertificateFromTruststoreResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DeleteCaCertificateFromTruststoreResponse> deleteCaCertificateFromTruststoreWithHttpInfo(@jakarta.annotation.Nonnull DeleteCaCertificateFromTruststoreRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteCaCertificateFromTruststore");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<DeleteCaCertificateFromTruststoreResponse> localVarReturnType = new GenericType<DeleteCaCertificateFromTruststoreResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteCaCertificateFromTruststore", "/api/v1/environments2/deleteCaCertificateFromTruststore", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes a credential.
   * Deletes a credential.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object deleteCredential(@jakarta.annotation.Nonnull DeleteCredentialRequest input) throws ApiException {
    return deleteCredentialWithHttpInfo(input).getData();
  }

  /**
   * Deletes a credential.
   * Deletes a credential.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> deleteCredentialWithHttpInfo(@jakarta.annotation.Nonnull DeleteCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteCredential", "/api/v1/environments2/deleteCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes an encryption profile.
   * Deletes an encryption profile.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object deleteEncryptionProfile(@jakarta.annotation.Nonnull DeleteEncryptionProfileRequest input) throws ApiException {
    return deleteEncryptionProfileWithHttpInfo(input).getData();
  }

  /**
   * Deletes an encryption profile.
   * Deletes an encryption profile.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> deleteEncryptionProfileWithHttpInfo(@jakarta.annotation.Nonnull DeleteEncryptionProfileRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteEncryptionProfile");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteEncryptionProfile", "/api/v1/environments2/deleteEncryptionProfile", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes an environment.
   * Deletes an environment.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object deleteEnvironment(@jakarta.annotation.Nonnull DeleteEnvironmentRequest input) throws ApiException {
    return deleteEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Deletes an environment.
   * Deletes an environment.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> deleteEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull DeleteEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteEnvironment", "/api/v1/environments2/deleteEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes all ID Broker mappings for an environment.
   * Deletes all ID Broker mappings for an environment.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object deleteIdBrokerMappings(@jakarta.annotation.Nonnull DeleteIdBrokerMappingsRequest input) throws ApiException {
    return deleteIdBrokerMappingsWithHttpInfo(input).getData();
  }

  /**
   * Deletes all ID Broker mappings for an environment.
   * Deletes all ID Broker mappings for an environment.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> deleteIdBrokerMappingsWithHttpInfo(@jakarta.annotation.Nonnull DeleteIdBrokerMappingsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteIdBrokerMappings");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteIdBrokerMappings", "/api/v1/environments2/deleteIdBrokerMappings", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Deletes a proxy config.
   * Deletes a proxy config.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object deleteProxyConfig(@jakarta.annotation.Nonnull DeleteProxyConfigRequest input) throws ApiException {
    return deleteProxyConfigWithHttpInfo(input).getData();
  }

  /**
   * Deletes a proxy config.
   * Deletes a proxy config.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> deleteProxyConfigWithHttpInfo(@jakarta.annotation.Nonnull DeleteProxyConfigRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling deleteProxyConfig");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.deleteProxyConfig", "/api/v1/environments2/deleteProxyConfig", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Describes an environment.
   * Describes an environment.
   * @param input  (required)
   * @return DescribeEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public DescribeEnvironmentResponse describeEnvironment(@jakarta.annotation.Nonnull DescribeEnvironmentRequest input) throws ApiException {
    return describeEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Describes an environment.
   * Describes an environment.
   * @param input  (required)
   * @return ApiResponse&lt;DescribeEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DescribeEnvironmentResponse> describeEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull DescribeEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling describeEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<DescribeEnvironmentResponse> localVarReturnType = new GenericType<DescribeEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.describeEnvironment", "/api/v1/environments2/describeEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Detach recipes from FreeIPA.
   * Detach recipes from FreeIPA.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object detachFreeIpaRecipes(@jakarta.annotation.Nonnull DetachFreeIpaRecipesRequest input) throws ApiException {
    return detachFreeIpaRecipesWithHttpInfo(input).getData();
  }

  /**
   * Detach recipes from FreeIPA.
   * Detach recipes from FreeIPA.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> detachFreeIpaRecipesWithHttpInfo(@jakarta.annotation.Nonnull DetachFreeIpaRecipesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling detachFreeIpaRecipes");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.detachFreeIpaRecipes", "/api/v1/environments2/detachFreeIpaRecipes", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Disable the S3Guard for an environment.
   * Deprecated, S3Guard is no longer supported.
   * @param input  (required)
   * @return DisableS3GuardResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public DisableS3GuardResponse disableS3Guard(@jakarta.annotation.Nonnull DisableS3GuardRequest input) throws ApiException {
    return disableS3GuardWithHttpInfo(input).getData();
  }

  /**
   * Disable the S3Guard for an environment.
   * Deprecated, S3Guard is no longer supported.
   * @param input  (required)
   * @return ApiResponse&lt;DisableS3GuardResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DisableS3GuardResponse> disableS3GuardWithHttpInfo(@jakarta.annotation.Nonnull DisableS3GuardRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling disableS3Guard");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<DisableS3GuardResponse> localVarReturnType = new GenericType<DisableS3GuardResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.disableS3Guard", "/api/v1/environments2/disableS3Guard", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Downscales FreeIPA instances.
   * FreeIPA is an integrated Identity and Authentication solution that can be used for any of CM, CDP services.
   * @param input  (required)
   * @return DownscaleFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public DownscaleFreeipaResponse downscaleFreeipa(@jakarta.annotation.Nonnull DownscaleFreeipaRequest input) throws ApiException {
    return downscaleFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Downscales FreeIPA instances.
   * FreeIPA is an integrated Identity and Authentication solution that can be used for any of CM, CDP services.
   * @param input  (required)
   * @return ApiResponse&lt;DownscaleFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DownscaleFreeipaResponse> downscaleFreeipaWithHttpInfo(@jakarta.annotation.Nonnull DownscaleFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling downscaleFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<DownscaleFreeipaResponse> localVarReturnType = new GenericType<DownscaleFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.downscaleFreeipa", "/api/v1/environments2/downscaleFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Extracts a portion of the image catalog currently used by the environment containing only the FreeIPA image that is in use.
   * Extracts a portion of the image catalog currently used by the environment containing only the FreeIPA image that is in use.
   * @param input  (required)
   * @return ExtractCatalogResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ExtractCatalogResponse extractCatalog(@jakarta.annotation.Nonnull ExtractCatalogRequest input) throws ApiException {
    return extractCatalogWithHttpInfo(input).getData();
  }

  /**
   * Extracts a portion of the image catalog currently used by the environment containing only the FreeIPA image that is in use.
   * Extracts a portion of the image catalog currently used by the environment containing only the FreeIPA image that is in use.
   * @param input  (required)
   * @return ApiResponse&lt;ExtractCatalogResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ExtractCatalogResponse> extractCatalogWithHttpInfo(@jakarta.annotation.Nonnull ExtractCatalogRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling extractCatalog");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ExtractCatalogResponse> localVarReturnType = new GenericType<ExtractCatalogResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.extractCatalog", "/api/v1/environments2/extractCatalog", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Complete cross-realm trust setup for FreeIPA.
   * Completes and validates the cross-realm trust setup initiated by setupTrust. Call this after configuring the Active Directory side of the trust relationship. This operation tests the bidirectional trust and activates it for production use.
   * @param input  (required)
   * @return FinishSetupTrustResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public FinishSetupTrustResponse finishSetupTrust(@jakarta.annotation.Nonnull FinishSetupTrustRequest input) throws ApiException {
    return finishSetupTrustWithHttpInfo(input).getData();
  }

  /**
   * Complete cross-realm trust setup for FreeIPA.
   * Completes and validates the cross-realm trust setup initiated by setupTrust. Call this after configuring the Active Directory side of the trust relationship. This operation tests the bidirectional trust and activates it for production use.
   * @param input  (required)
   * @return ApiResponse&lt;FinishSetupTrustResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<FinishSetupTrustResponse> finishSetupTrustWithHttpInfo(@jakarta.annotation.Nonnull FinishSetupTrustRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling finishSetupTrust");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<FinishSetupTrustResponse> localVarReturnType = new GenericType<FinishSetupTrustResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.finishSetupTrust", "/api/v1/environments2/finishSetupTrust", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get account level telemetry settings. (telemetry features and anonymization rules)
   * Get account level telemetry settings. (telemetry features and anonymization rules)
   * @param input  (required)
   * @return GetAccountTelemetryResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetAccountTelemetryResponse getAccountTelemetry(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return getAccountTelemetryWithHttpInfo(input).getData();
  }

  /**
   * Get account level telemetry settings. (telemetry features and anonymization rules)
   * Get account level telemetry settings. (telemetry features and anonymization rules)
   * @param input  (required)
   * @return ApiResponse&lt;GetAccountTelemetryResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetAccountTelemetryResponse> getAccountTelemetryWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getAccountTelemetry");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetAccountTelemetryResponse> localVarReturnType = new GenericType<GetAccountTelemetryResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getAccountTelemetry", "/api/v1/environments2/getAccountTelemetry", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get default account level telemetry settings. Helps to set back the default values.
   * Get default account level telemetry settings. Helps to set back the default values.
   * @param input  (required)
   * @return GetAccountTelemetryDefaultResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetAccountTelemetryDefaultResponse getAccountTelemetryDefault(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return getAccountTelemetryDefaultWithHttpInfo(input).getData();
  }

  /**
   * Get default account level telemetry settings. Helps to set back the default values.
   * Get default account level telemetry settings. Helps to set back the default values.
   * @param input  (required)
   * @return ApiResponse&lt;GetAccountTelemetryDefaultResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetAccountTelemetryDefaultResponse> getAccountTelemetryDefaultWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getAccountTelemetryDefault");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetAccountTelemetryDefaultResponse> localVarReturnType = new GenericType<GetAccountTelemetryDefaultResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getAccountTelemetryDefault", "/api/v1/environments2/getAccountTelemetryDefault", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * This API provides the audit credential prerequisites for the given cloud provider.
   * Provides the the audit credential prerequisites for the given cloud provider.
   * @param input  (required)
   * @return GetAuditCredentialPrerequisitesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetAuditCredentialPrerequisitesResponse getAuditCredentialPrerequisites(@jakarta.annotation.Nonnull GetAuditCredentialPrerequisitesRequest input) throws ApiException {
    return getAuditCredentialPrerequisitesWithHttpInfo(input).getData();
  }

  /**
   * This API provides the audit credential prerequisites for the given cloud provider.
   * Provides the the audit credential prerequisites for the given cloud provider.
   * @param input  (required)
   * @return ApiResponse&lt;GetAuditCredentialPrerequisitesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetAuditCredentialPrerequisitesResponse> getAuditCredentialPrerequisitesWithHttpInfo(@jakarta.annotation.Nonnull GetAuditCredentialPrerequisitesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getAuditCredentialPrerequisites");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetAuditCredentialPrerequisitesResponse> localVarReturnType = new GenericType<GetAuditCredentialPrerequisitesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getAuditCredentialPrerequisites", "/api/v1/environments2/getAuditCredentialPrerequisites", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Gets the the automated sync status for the environment.
   * Gets the the automated sync status for the environment.
   * @param input  (required)
   * @return GetAutomatedSyncEnvironmentStatusResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetAutomatedSyncEnvironmentStatusResponse getAutomatedSyncEnvironmentStatus(@jakarta.annotation.Nonnull GetAutomatedSyncEnvironmentStatusRequest input) throws ApiException {
    return getAutomatedSyncEnvironmentStatusWithHttpInfo(input).getData();
  }

  /**
   * Gets the the automated sync status for the environment.
   * Gets the the automated sync status for the environment.
   * @param input  (required)
   * @return ApiResponse&lt;GetAutomatedSyncEnvironmentStatusResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetAutomatedSyncEnvironmentStatusResponse> getAutomatedSyncEnvironmentStatusWithHttpInfo(@jakarta.annotation.Nonnull GetAutomatedSyncEnvironmentStatusRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getAutomatedSyncEnvironmentStatus");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetAutomatedSyncEnvironmentStatusResponse> localVarReturnType = new GenericType<GetAutomatedSyncEnvironmentStatusResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getAutomatedSyncEnvironmentStatus", "/api/v1/environments2/getAutomatedSyncEnvironmentStatus", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Gets account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * Gets account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * @param input  (required)
   * @return GetAzureImageTermsPolicyResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetAzureImageTermsPolicyResponse getAzureImageTermsPolicy(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return getAzureImageTermsPolicyWithHttpInfo(input).getData();
  }

  /**
   * Gets account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * Gets account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * @param input  (required)
   * @return ApiResponse&lt;GetAzureImageTermsPolicyResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetAzureImageTermsPolicyResponse> getAzureImageTermsPolicyWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getAzureImageTermsPolicy");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetAzureImageTermsPolicyResponse> localVarReturnType = new GenericType<GetAzureImageTermsPolicyResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getAzureImageTermsPolicy", "/api/v1/environments2/getAzureImageTermsPolicy", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get create credential CLI command based on private API request or response object
   * Get create credential CLI command based on private API request or response object
   * @param input  (required)
   * @return GetCliForCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Create credential CLI command for valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetCliForCredentialResponse getCliForCredential(@jakarta.annotation.Nonnull GetCliForCredentialRequest input) throws ApiException {
    return getCliForCredentialWithHttpInfo(input).getData();
  }

  /**
   * Get create credential CLI command based on private API request or response object
   * Get create credential CLI command based on private API request or response object
   * @param input  (required)
   * @return ApiResponse&lt;GetCliForCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Create credential CLI command for valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetCliForCredentialResponse> getCliForCredentialWithHttpInfo(@jakarta.annotation.Nonnull GetCliForCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getCliForCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetCliForCredentialResponse> localVarReturnType = new GenericType<GetCliForCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getCliForCredential", "/api/v1/environments2/getCliForCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get create environment CLI command based on private API request or response object
   * Get create environment CLI command based on private API request or response object
   * @param input  (required)
   * @return GetCliForEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Create environment CLI command for valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetCliForEnvironmentResponse getCliForEnvironment(@jakarta.annotation.Nonnull GetCliForEnvironmentRequest input) throws ApiException {
    return getCliForEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Get create environment CLI command based on private API request or response object
   * Get create environment CLI command based on private API request or response object
   * @param input  (required)
   * @return ApiResponse&lt;GetCliForEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Create environment CLI command for valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetCliForEnvironmentResponse> getCliForEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull GetCliForEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getCliForEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetCliForEnvironmentResponse> localVarReturnType = new GenericType<GetCliForEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getCliForEnvironment", "/api/v1/environments2/getCliForEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the client configs for a Datalake.
   * Get the client configs for a Datalake.
   * @param input  (required)
   * @return GetConfigFilesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> search results matching criteria </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetConfigFilesResponse getConfigFiles(@jakarta.annotation.Nonnull GetConfigFilesRequest input) throws ApiException {
    return getConfigFilesWithHttpInfo(input).getData();
  }

  /**
   * Get the client configs for a Datalake.
   * Get the client configs for a Datalake.
   * @param input  (required)
   * @return ApiResponse&lt;GetConfigFilesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> search results matching criteria </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetConfigFilesResponse> getConfigFilesWithHttpInfo(@jakarta.annotation.Nonnull GetConfigFilesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getConfigFiles");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetConfigFilesResponse> localVarReturnType = new GenericType<GetConfigFilesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getConfigFiles", "/api/v1/environments2/getConfigFiles", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * This API provides the credential prerequisites for the given cloud provider.
   * Provides the the credential prerequisites for the given cloud provider.
   * @param input  (required)
   * @return GetCredentialPrerequisitesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetCredentialPrerequisitesResponse getCredentialPrerequisites(@jakarta.annotation.Nonnull GetCredentialPrerequisitesRequest input) throws ApiException {
    return getCredentialPrerequisitesWithHttpInfo(input).getData();
  }

  /**
   * This API provides the credential prerequisites for the given cloud provider.
   * Provides the the credential prerequisites for the given cloud provider.
   * @param input  (required)
   * @return ApiResponse&lt;GetCredentialPrerequisitesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetCredentialPrerequisitesResponse> getCredentialPrerequisitesWithHttpInfo(@jakarta.annotation.Nonnull GetCredentialPrerequisitesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getCredentialPrerequisites");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetCredentialPrerequisitesResponse> localVarReturnType = new GenericType<GetCredentialPrerequisitesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getCredentialPrerequisites", "/api/v1/environments2/getCredentialPrerequisites", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Read a configuration setting from the environment service.
   * Read a configuration setting from the environment service.
   * @param input  (required)
   * @return GetEnvironmentSettingResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetEnvironmentSettingResponse getEnvironmentSetting(@jakarta.annotation.Nonnull GetEnvironmentSettingRequest input) throws ApiException {
    return getEnvironmentSettingWithHttpInfo(input).getData();
  }

  /**
   * Read a configuration setting from the environment service.
   * Read a configuration setting from the environment service.
   * @param input  (required)
   * @return ApiResponse&lt;GetEnvironmentSettingResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetEnvironmentSettingResponse> getEnvironmentSettingWithHttpInfo(@jakarta.annotation.Nonnull GetEnvironmentSettingRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getEnvironmentSetting");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetEnvironmentSettingResponse> localVarReturnType = new GenericType<GetEnvironmentSettingResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getEnvironmentSetting", "/api/v1/environments2/getEnvironmentSetting", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Returns the user synchronization state for an environment.
   * Returns the user synchronization state for an environment.
   * @param input  (required)
   * @return GetEnvironmentUserSyncStateResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetEnvironmentUserSyncStateResponse getEnvironmentUserSyncState(@jakarta.annotation.Nonnull GetEnvironmentUserSyncStateRequest input) throws ApiException {
    return getEnvironmentUserSyncStateWithHttpInfo(input).getData();
  }

  /**
   * Returns the user synchronization state for an environment.
   * Returns the user synchronization state for an environment.
   * @param input  (required)
   * @return ApiResponse&lt;GetEnvironmentUserSyncStateResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetEnvironmentUserSyncStateResponse> getEnvironmentUserSyncStateWithHttpInfo(@jakarta.annotation.Nonnull GetEnvironmentUserSyncStateRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getEnvironmentUserSyncState");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetEnvironmentUserSyncStateResponse> localVarReturnType = new GenericType<GetEnvironmentUserSyncStateResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getEnvironmentUserSyncState", "/api/v1/environments2/getEnvironmentUserSyncState", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Gather log descriptors that are used for diagnostics collection.
   * Gather log descriptors that are used for diagnostics collection.
   * @param input  (required)
   * @return GetFreeipaLogDescriptorsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetFreeipaLogDescriptorsResponse getFreeipaLogDescriptors(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return getFreeipaLogDescriptorsWithHttpInfo(input).getData();
  }

  /**
   * Gather log descriptors that are used for diagnostics collection.
   * Gather log descriptors that are used for diagnostics collection.
   * @param input  (required)
   * @return ApiResponse&lt;GetFreeipaLogDescriptorsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetFreeipaLogDescriptorsResponse> getFreeipaLogDescriptorsWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getFreeipaLogDescriptors");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetFreeipaLogDescriptorsResponse> localVarReturnType = new GenericType<GetFreeipaLogDescriptorsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getFreeipaLogDescriptors", "/api/v1/environments2/getFreeipaLogDescriptors", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the secret encryption keys&#39; IDs of the FreeIPA cluster.
   * Get the secret encryption keys&#39; IDs of the FreeIPA cluster.
   * @param input  (required)
   * @return GetFreeipaSecretEncryptionKeyIdsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetFreeipaSecretEncryptionKeyIdsResponse getFreeipaSecretEncryptionKeyIds(@jakarta.annotation.Nonnull GetFreeipaSecretEncryptionKeyIdsRequest input) throws ApiException {
    return getFreeipaSecretEncryptionKeyIdsWithHttpInfo(input).getData();
  }

  /**
   * Get the secret encryption keys&#39; IDs of the FreeIPA cluster.
   * Get the secret encryption keys&#39; IDs of the FreeIPA cluster.
   * @param input  (required)
   * @return ApiResponse&lt;GetFreeipaSecretEncryptionKeyIdsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetFreeipaSecretEncryptionKeyIdsResponse> getFreeipaSecretEncryptionKeyIdsWithHttpInfo(@jakarta.annotation.Nonnull GetFreeipaSecretEncryptionKeyIdsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getFreeipaSecretEncryptionKeyIds");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetFreeipaSecretEncryptionKeyIdsResponse> localVarReturnType = new GenericType<GetFreeipaSecretEncryptionKeyIdsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getFreeipaSecretEncryptionKeyIds", "/api/v1/environments2/getFreeipaSecretEncryptionKeyIds", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the status of the FreeIPA services
   * Gets the status of the FreeIPA nodes services and connectivity.
   * @param input  (required)
   * @return GetFreeipaStatusResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetFreeipaStatusResponse getFreeipaStatus(@jakarta.annotation.Nonnull GetFreeipaStatusRequest input) throws ApiException {
    return getFreeipaStatusWithHttpInfo(input).getData();
  }

  /**
   * Get the status of the FreeIPA services
   * Gets the status of the FreeIPA nodes services and connectivity.
   * @param input  (required)
   * @return ApiResponse&lt;GetFreeipaStatusResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetFreeipaStatusResponse> getFreeipaStatusWithHttpInfo(@jakarta.annotation.Nonnull GetFreeipaStatusRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getFreeipaStatus");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetFreeipaStatusResponse> localVarReturnType = new GenericType<GetFreeipaStatusResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getFreeipaStatus", "/api/v1/environments2/getFreeipaStatus", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get available images for FreeIPA upgrade.
   * Get available images for FreeIPA upgrade. If catalog is defined use the catalog as image source.
   * @param input  (required)
   * @return GetFreeipaUpgradeOptionsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetFreeipaUpgradeOptionsResponse getFreeipaUpgradeOptions(@jakarta.annotation.Nonnull GetFreeipaUpgradeOptionsRequest input) throws ApiException {
    return getFreeipaUpgradeOptionsWithHttpInfo(input).getData();
  }

  /**
   * Get available images for FreeIPA upgrade.
   * Get available images for FreeIPA upgrade. If catalog is defined use the catalog as image source.
   * @param input  (required)
   * @return ApiResponse&lt;GetFreeipaUpgradeOptionsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetFreeipaUpgradeOptionsResponse> getFreeipaUpgradeOptionsWithHttpInfo(@jakarta.annotation.Nonnull GetFreeipaUpgradeOptionsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getFreeipaUpgradeOptions");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetFreeipaUpgradeOptionsResponse> localVarReturnType = new GenericType<GetFreeipaUpgradeOptionsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getFreeipaUpgradeOptions", "/api/v1/environments2/getFreeipaUpgradeOptions", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * This API provides the audit credential prerequisites for GovCloud for the enabled providers.
   * Provides the the audit credential prerequisites for GovCloud for the enabled providers.
   * @param input  (required)
   * @return GetGovCloudAuditCredentialPrerequisitesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetGovCloudAuditCredentialPrerequisitesResponse getGovCloudAuditCredentialPrerequisites(@jakarta.annotation.Nonnull GetGovCloudAuditCredentialPrerequisitesRequest input) throws ApiException {
    return getGovCloudAuditCredentialPrerequisitesWithHttpInfo(input).getData();
  }

  /**
   * This API provides the audit credential prerequisites for GovCloud for the enabled providers.
   * Provides the the audit credential prerequisites for GovCloud for the enabled providers.
   * @param input  (required)
   * @return ApiResponse&lt;GetGovCloudAuditCredentialPrerequisitesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetGovCloudAuditCredentialPrerequisitesResponse> getGovCloudAuditCredentialPrerequisitesWithHttpInfo(@jakarta.annotation.Nonnull GetGovCloudAuditCredentialPrerequisitesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getGovCloudAuditCredentialPrerequisites");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetGovCloudAuditCredentialPrerequisitesResponse> localVarReturnType = new GenericType<GetGovCloudAuditCredentialPrerequisitesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getGovCloudAuditCredentialPrerequisites", "/api/v1/environments2/getGovCloudAuditCredentialPrerequisites", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * This API provides the credential prerequisites for GovCloud for the enabled providers.
   * Provides the the credential prerequisites for GovCloud for the enabled providers.
   * @param input  (required)
   * @return GetGovCloudCredentialPrerequisitesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetGovCloudCredentialPrerequisitesResponse getGovCloudCredentialPrerequisites(@jakarta.annotation.Nonnull GetGovCloudCredentialPrerequisitesRequest input) throws ApiException {
    return getGovCloudCredentialPrerequisitesWithHttpInfo(input).getData();
  }

  /**
   * This API provides the credential prerequisites for GovCloud for the enabled providers.
   * Provides the the credential prerequisites for GovCloud for the enabled providers.
   * @param input  (required)
   * @return ApiResponse&lt;GetGovCloudCredentialPrerequisitesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetGovCloudCredentialPrerequisitesResponse> getGovCloudCredentialPrerequisitesWithHttpInfo(@jakarta.annotation.Nonnull GetGovCloudCredentialPrerequisitesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getGovCloudCredentialPrerequisites");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetGovCloudCredentialPrerequisitesResponse> localVarReturnType = new GenericType<GetGovCloudCredentialPrerequisitesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getGovCloudCredentialPrerequisites", "/api/v1/environments2/getGovCloudCredentialPrerequisites", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Gets all ID Broker mappings for an environment.
   * Gets all ID Broker mappings for an environment.
   * @param input  (required)
   * @return GetIdBrokerMappingsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetIdBrokerMappingsResponse getIdBrokerMappings(@jakarta.annotation.Nonnull GetIdBrokerMappingsRequest input) throws ApiException {
    return getIdBrokerMappingsWithHttpInfo(input).getData();
  }

  /**
   * Gets all ID Broker mappings for an environment.
   * Gets all ID Broker mappings for an environment.
   * @param input  (required)
   * @return ApiResponse&lt;GetIdBrokerMappingsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetIdBrokerMappingsResponse> getIdBrokerMappingsWithHttpInfo(@jakarta.annotation.Nonnull GetIdBrokerMappingsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getIdBrokerMappings");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetIdBrokerMappingsResponse> localVarReturnType = new GenericType<GetIdBrokerMappingsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getIdBrokerMappings", "/api/v1/environments2/getIdBrokerMappings", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Endpoint to obtain the cli command to be able to recreate the IDBroker mapping of the given environment.
   * Get cli command to be able to recreate the IDBroker mapping of the given environment.
   * @param input  (required)
   * @return GetIdBrokerMappingsCliForEnvResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Response object which contains the valid CLI command to (re)create the IDBroker mapping of the given environment. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetIdBrokerMappingsCliForEnvResponse getIdBrokerMappingsCliForEnv(@jakarta.annotation.Nonnull GetIdBrokerMappingsCliForEnvRequest input) throws ApiException {
    return getIdBrokerMappingsCliForEnvWithHttpInfo(input).getData();
  }

  /**
   * Endpoint to obtain the cli command to be able to recreate the IDBroker mapping of the given environment.
   * Get cli command to be able to recreate the IDBroker mapping of the given environment.
   * @param input  (required)
   * @return ApiResponse&lt;GetIdBrokerMappingsCliForEnvResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Response object which contains the valid CLI command to (re)create the IDBroker mapping of the given environment. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetIdBrokerMappingsCliForEnvResponse> getIdBrokerMappingsCliForEnvWithHttpInfo(@jakarta.annotation.Nonnull GetIdBrokerMappingsCliForEnvRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getIdBrokerMappingsCliForEnv");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetIdBrokerMappingsCliForEnvResponse> localVarReturnType = new GenericType<GetIdBrokerMappingsCliForEnvResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getIdBrokerMappingsCliForEnv", "/api/v1/environments2/getIdBrokerMappingsCliForEnv", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Gets ID Broker mappings sync status.
   * Gets the status of the most recent ID Broker mappings sync operation, if any.
   * @param input  (required)
   * @return GetIdBrokerMappingsSyncStatusResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetIdBrokerMappingsSyncStatusResponse getIdBrokerMappingsSyncStatus(@jakarta.annotation.Nonnull GetIdBrokerMappingsSyncStatusRequest input) throws ApiException {
    return getIdBrokerMappingsSyncStatusWithHttpInfo(input).getData();
  }

  /**
   * Gets ID Broker mappings sync status.
   * Gets the status of the most recent ID Broker mappings sync operation, if any.
   * @param input  (required)
   * @return ApiResponse&lt;GetIdBrokerMappingsSyncStatusResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetIdBrokerMappingsSyncStatusResponse> getIdBrokerMappingsSyncStatusWithHttpInfo(@jakarta.annotation.Nonnull GetIdBrokerMappingsSyncStatusRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getIdBrokerMappingsSyncStatus");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetIdBrokerMappingsSyncStatusResponse> localVarReturnType = new GenericType<GetIdBrokerMappingsSyncStatusResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getIdBrokerMappingsSyncStatus", "/api/v1/environments2/getIdBrokerMappingsSyncStatus", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Retrieves a keytab for a user or machine user.
   * Retrieves a keytab for a user or machine user.
   * @param input  (required)
   * @return GetKeytabResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetKeytabResponse getKeytab(@jakarta.annotation.Nonnull GetKeytabRequest input) throws ApiException {
    return getKeytabWithHttpInfo(input).getData();
  }

  /**
   * Retrieves a keytab for a user or machine user.
   * Retrieves a keytab for a user or machine user.
   * @param input  (required)
   * @return ApiResponse&lt;GetKeytabResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetKeytabResponse> getKeytabWithHttpInfo(@jakarta.annotation.Nonnull GetKeytabRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getKeytab");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetKeytabResponse> localVarReturnType = new GenericType<GetKeytabResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getKeytab", "/api/v1/environments2/getKeytab", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the latest (in progress or finished) operation for the environment resource.
   * Get the latest (in progress or finished) operation for the environment resource.
   * @param input  (required)
   * @return GetOperationResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetOperationResponse getOperation(@jakarta.annotation.Nonnull GetOperationRequest input) throws ApiException {
    return getOperationWithHttpInfo(input).getData();
  }

  /**
   * Get the latest (in progress or finished) operation for the environment resource.
   * Get the latest (in progress or finished) operation for the environment resource.
   * @param input  (required)
   * @return ApiResponse&lt;GetOperationResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetOperationResponse> getOperationWithHttpInfo(@jakarta.annotation.Nonnull GetOperationRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getOperation");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetOperationResponse> localVarReturnType = new GenericType<GetOperationResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getOperation", "/api/v1/environments2/getOperation", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Returns status of the repair operation for the operation id.
   * Returns status of the repair operation for the operation id. Operation Id should be one of the previously requested repair operation ids.
   * @param input  (required)
   * @return GetRepairFreeipaStatusResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetRepairFreeipaStatusResponse getRepairFreeipaStatus(@jakarta.annotation.Nonnull GetRepairFreeipaStatusRequest input) throws ApiException {
    return getRepairFreeipaStatusWithHttpInfo(input).getData();
  }

  /**
   * Returns status of the repair operation for the operation id.
   * Returns status of the repair operation for the operation id. Operation Id should be one of the previously requested repair operation ids.
   * @param input  (required)
   * @return ApiResponse&lt;GetRepairFreeipaStatusResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetRepairFreeipaStatusResponse> getRepairFreeipaStatusWithHttpInfo(@jakarta.annotation.Nonnull GetRepairFreeipaStatusRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getRepairFreeipaStatus");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetRepairFreeipaStatusResponse> localVarReturnType = new GenericType<GetRepairFreeipaStatusResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getRepairFreeipaStatus", "/api/v1/environments2/getRepairFreeipaStatus", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * This API provides contents of public certificate for an environment.
   * This API provides the contents of the root public certificate for an environment. The contents are a base64 encoded blob between -----BEGIN CERTIFICATE----- and -----END CERTIFICATE-----. This certificate can be imported by end users to establish trust with environment resources.
   * @param input  (required)
   * @return GetRootCertificateResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetRootCertificateResponse getRootCertificate(@jakarta.annotation.Nonnull GetRootCertificateRequest input) throws ApiException {
    return getRootCertificateWithHttpInfo(input).getData();
  }

  /**
   * This API provides contents of public certificate for an environment.
   * This API provides the contents of the root public certificate for an environment. The contents are a base64 encoded blob between -----BEGIN CERTIFICATE----- and -----END CERTIFICATE-----. This certificate can be imported by end users to establish trust with environment resources.
   * @param input  (required)
   * @return ApiResponse&lt;GetRootCertificateResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetRootCertificateResponse> getRootCertificateWithHttpInfo(@jakarta.annotation.Nonnull GetRootCertificateRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getRootCertificate");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetRootCertificateResponse> localVarReturnType = new GenericType<GetRootCertificateResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getRootCertificate", "/api/v1/environments2/getRootCertificate", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the truststore password for a Datalake.
   * Get the truststore password for a Datalake.
   * @param input  (required)
   * @return GetTruststorePasswordResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> search results matching criteria </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public GetTruststorePasswordResponse getTruststorePassword(@jakarta.annotation.Nonnull GetTruststorePasswordRequest input) throws ApiException {
    return getTruststorePasswordWithHttpInfo(input).getData();
  }

  /**
   * Get the truststore password for a Datalake.
   * Get the truststore password for a Datalake.
   * @param input  (required)
   * @return ApiResponse&lt;GetTruststorePasswordResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> search results matching criteria </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<GetTruststorePasswordResponse> getTruststorePasswordWithHttpInfo(@jakarta.annotation.Nonnull GetTruststorePasswordRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling getTruststorePassword");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<GetTruststorePasswordResponse> localVarReturnType = new GenericType<GetTruststorePasswordResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.getTruststorePassword", "/api/v1/environments2/getTruststorePassword", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Initialize compute cluster for AWS environment.
   * Initialize compute cluster for AWS environment.
   * @param input  (required)
   * @return InitializeAWSComputeClusterResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public InitializeAWSComputeClusterResponse initializeAWSComputeCluster(@jakarta.annotation.Nonnull InitializeAWSComputeClusterRequest input) throws ApiException {
    return initializeAWSComputeClusterWithHttpInfo(input).getData();
  }

  /**
   * Initialize compute cluster for AWS environment.
   * Initialize compute cluster for AWS environment.
   * @param input  (required)
   * @return ApiResponse&lt;InitializeAWSComputeClusterResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<InitializeAWSComputeClusterResponse> initializeAWSComputeClusterWithHttpInfo(@jakarta.annotation.Nonnull InitializeAWSComputeClusterRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling initializeAWSComputeCluster");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<InitializeAWSComputeClusterResponse> localVarReturnType = new GenericType<InitializeAWSComputeClusterResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.initializeAWSComputeCluster", "/api/v1/environments2/initializeAWSComputeCluster", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Initialize compute cluster for Azure environment.
   * Initialize compute cluster for Azure environment.
   * @param input  (required)
   * @return InitializeAzureComputeClusterResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public InitializeAzureComputeClusterResponse initializeAzureComputeCluster(@jakarta.annotation.Nonnull InitializeAzureComputeClusterRequest input) throws ApiException {
    return initializeAzureComputeClusterWithHttpInfo(input).getData();
  }

  /**
   * Initialize compute cluster for Azure environment.
   * Initialize compute cluster for Azure environment.
   * @param input  (required)
   * @return ApiResponse&lt;InitializeAzureComputeClusterResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<InitializeAzureComputeClusterResponse> initializeAzureComputeClusterWithHttpInfo(@jakarta.annotation.Nonnull InitializeAzureComputeClusterRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling initializeAzureComputeCluster");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<InitializeAzureComputeClusterResponse> localVarReturnType = new GenericType<InitializeAzureComputeClusterResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.initializeAzureComputeCluster", "/api/v1/environments2/initializeAzureComputeCluster", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Returns status of the sync operation for the environment crn.
   * Returns status of the sync operation for the environment based on crn.
   * @param input  (required)
   * @return LastSyncStatusResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public LastSyncStatusResponse lastSyncStatus(@jakarta.annotation.Nonnull LastSyncStatusRequest input) throws ApiException {
    return lastSyncStatusWithHttpInfo(input).getData();
  }

  /**
   * Returns status of the sync operation for the environment crn.
   * Returns status of the sync operation for the environment based on crn.
   * @param input  (required)
   * @return ApiResponse&lt;LastSyncStatusResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<LastSyncStatusResponse> lastSyncStatusWithHttpInfo(@jakarta.annotation.Nonnull LastSyncStatusRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling lastSyncStatus");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<LastSyncStatusResponse> localVarReturnType = new GenericType<LastSyncStatusResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.lastSyncStatus", "/api/v1/environments2/lastSyncStatus", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists audit credentials.
   * Lists audit credentials.
   * @param input  (required)
   * @return ListAuditCredentialsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListAuditCredentialsResponse listAuditCredentials(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return listAuditCredentialsWithHttpInfo(input).getData();
  }

  /**
   * Lists audit credentials.
   * Lists audit credentials.
   * @param input  (required)
   * @return ApiResponse&lt;ListAuditCredentialsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListAuditCredentialsResponse> listAuditCredentialsWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listAuditCredentials");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListAuditCredentialsResponse> localVarReturnType = new GenericType<ListAuditCredentialsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listAuditCredentials", "/api/v1/environments2/listAuditCredentials", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Returns the list of Data Services and their cluster names that are attached the given environment.
   * Returns the list of Data Services and their cluster names that are attached to the given environment.
   * @param input  (required)
   * @return ListConnectedDataServicesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListConnectedDataServicesResponse listConnectedDataServices(@jakarta.annotation.Nonnull ListConnectedDataServicesRequest input) throws ApiException {
    return listConnectedDataServicesWithHttpInfo(input).getData();
  }

  /**
   * Returns the list of Data Services and their cluster names that are attached the given environment.
   * Returns the list of Data Services and their cluster names that are attached to the given environment.
   * @param input  (required)
   * @return ApiResponse&lt;ListConnectedDataServicesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListConnectedDataServicesResponse> listConnectedDataServicesWithHttpInfo(@jakarta.annotation.Nonnull ListConnectedDataServicesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listConnectedDataServices");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListConnectedDataServicesResponse> localVarReturnType = new GenericType<ListConnectedDataServicesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listConnectedDataServices", "/api/v1/environments2/listConnectedDataServices", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists credentials.
   * Lists credentials.
   * @param input  (required)
   * @return ListCredentialsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListCredentialsResponse listCredentials(@jakarta.annotation.Nonnull ListCredentialsRequest input) throws ApiException {
    return listCredentialsWithHttpInfo(input).getData();
  }

  /**
   * Lists credentials.
   * Lists credentials.
   * @param input  (required)
   * @return ApiResponse&lt;ListCredentialsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListCredentialsResponse> listCredentialsWithHttpInfo(@jakarta.annotation.Nonnull ListCredentialsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listCredentials");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListCredentialsResponse> localVarReturnType = new GenericType<ListCredentialsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listCredentials", "/api/v1/environments2/listCredentials", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists all encryption profiles.
   * Lists all encryption profiles.
   * @param input  (required)
   * @return ListEncryptionProfilesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListEncryptionProfilesResponse listEncryptionProfiles(@jakarta.annotation.Nonnull ListEncryptionProfilesRequest input) throws ApiException {
    return listEncryptionProfilesWithHttpInfo(input).getData();
  }

  /**
   * Lists all encryption profiles.
   * Lists all encryption profiles.
   * @param input  (required)
   * @return ApiResponse&lt;ListEncryptionProfilesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListEncryptionProfilesResponse> listEncryptionProfilesWithHttpInfo(@jakarta.annotation.Nonnull ListEncryptionProfilesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listEncryptionProfiles");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListEncryptionProfilesResponse> localVarReturnType = new GenericType<ListEncryptionProfilesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listEncryptionProfiles", "/api/v1/environments2/listEncryptionProfiles", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists environments.
   * Lists environments.
   * @param input  (required)
   * @return ListEnvironmentsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListEnvironmentsResponse listEnvironments(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return listEnvironmentsWithHttpInfo(input).getData();
  }

  /**
   * Lists environments.
   * Lists environments.
   * @param input  (required)
   * @return ApiResponse&lt;ListEnvironmentsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListEnvironmentsResponse> listEnvironmentsWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listEnvironments");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListEnvironmentsResponse> localVarReturnType = new GenericType<ListEnvironmentsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listEnvironments", "/api/v1/environments2/listEnvironments", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * List recent FreeIPA diagnostics collections
   * List recent FreeIPA diagnostics collection
   * @param input  (required)
   * @return ListFreeipaDiagnosticsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListFreeipaDiagnosticsResponse listFreeipaDiagnostics(@jakarta.annotation.Nonnull ListFreeipaDiagnosticsRequest input) throws ApiException {
    return listFreeipaDiagnosticsWithHttpInfo(input).getData();
  }

  /**
   * List recent FreeIPA diagnostics collections
   * List recent FreeIPA diagnostics collection
   * @param input  (required)
   * @return ApiResponse&lt;ListFreeipaDiagnosticsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListFreeipaDiagnosticsResponse> listFreeipaDiagnosticsWithHttpInfo(@jakarta.annotation.Nonnull ListFreeipaDiagnosticsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listFreeipaDiagnostics");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListFreeipaDiagnosticsResponse> localVarReturnType = new GenericType<ListFreeipaDiagnosticsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listFreeipaDiagnostics", "/api/v1/environments2/listFreeipaDiagnostics", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists all FreeIPA related secret types.
   * Lists FreeIPA related secret types for FreeIPA instances.
   * @param input  (required)
   * @return ListFreeipaSecretTypesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListFreeipaSecretTypesResponse listFreeipaSecretTypes(@jakarta.annotation.Nonnull ListFreeipaSecretTypesRequest input) throws ApiException {
    return listFreeipaSecretTypesWithHttpInfo(input).getData();
  }

  /**
   * Lists all FreeIPA related secret types.
   * Lists FreeIPA related secret types for FreeIPA instances.
   * @param input  (required)
   * @return ApiResponse&lt;ListFreeipaSecretTypesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListFreeipaSecretTypesResponse> listFreeipaSecretTypesWithHttpInfo(@jakarta.annotation.Nonnull ListFreeipaSecretTypesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listFreeipaSecretTypes");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListFreeipaSecretTypesResponse> localVarReturnType = new GenericType<ListFreeipaSecretTypesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listFreeipaSecretTypes", "/api/v1/environments2/listFreeipaSecretTypes", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists proxy configs.
   * Lists proxy configs.
   * @param input  (required)
   * @return ListProxyConfigsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListProxyConfigsResponse listProxyConfigs(@jakarta.annotation.Nonnull ListProxyConfigsRequest input) throws ApiException {
    return listProxyConfigsWithHttpInfo(input).getData();
  }

  /**
   * Lists proxy configs.
   * Lists proxy configs.
   * @param input  (required)
   * @return ApiResponse&lt;ListProxyConfigsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListProxyConfigsResponse> listProxyConfigsWithHttpInfo(@jakarta.annotation.Nonnull ListProxyConfigsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listProxyConfigs");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListProxyConfigsResponse> localVarReturnType = new GenericType<ListProxyConfigsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listProxyConfigs", "/api/v1/environments2/listProxyConfigs", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Lists trust store certificates.
   * Lists trust store certificates.
   * @param input  (required)
   * @return ListTruststoreCertificatesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ListTruststoreCertificatesResponse listTruststoreCertificates(@jakarta.annotation.Nonnull ListTruststoreCertificatesRequest input) throws ApiException {
    return listTruststoreCertificatesWithHttpInfo(input).getData();
  }

  /**
   * Lists trust store certificates.
   * Lists trust store certificates.
   * @param input  (required)
   * @return ApiResponse&lt;ListTruststoreCertificatesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ListTruststoreCertificatesResponse> listTruststoreCertificatesWithHttpInfo(@jakarta.annotation.Nonnull ListTruststoreCertificatesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling listTruststoreCertificates");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ListTruststoreCertificatesResponse> localVarReturnType = new GenericType<ListTruststoreCertificatesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.listTruststoreCertificates", "/api/v1/environments2/listTruststoreCertificates", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Modifies SELinux mode on FreeIPA instances of the environment.
   * Modifies SELinux mode on FreeIPA instances of the environment.
   * @param input  (required)
   * @return ModifySelinuxResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ModifySelinuxResponse modifySelinux(@jakarta.annotation.Nonnull ModifySelinuxRequest input) throws ApiException {
    return modifySelinuxWithHttpInfo(input).getData();
  }

  /**
   * Modifies SELinux mode on FreeIPA instances of the environment.
   * Modifies SELinux mode on FreeIPA instances of the environment.
   * @param input  (required)
   * @return ApiResponse&lt;ModifySelinuxResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModifySelinuxResponse> modifySelinuxWithHttpInfo(@jakarta.annotation.Nonnull ModifySelinuxRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling modifySelinux");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ModifySelinuxResponse> localVarReturnType = new GenericType<ModifySelinuxResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.modifySelinux", "/api/v1/environments2/modifySelinux", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Recreate FreeIPA in case of disaster.
   * If FreeIPA backups are available and all of the FreeIPA nodes are lost, this command recreates FreeIPA from scratch and restores backup.
   * @param input  (required)
   * @return RebuildFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public RebuildFreeipaResponse rebuildFreeipa(@jakarta.annotation.Nonnull RebuildFreeipaRequest input) throws ApiException {
    return rebuildFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Recreate FreeIPA in case of disaster.
   * If FreeIPA backups are available and all of the FreeIPA nodes are lost, this command recreates FreeIPA from scratch and restores backup.
   * @param input  (required)
   * @return ApiResponse&lt;RebuildFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<RebuildFreeipaResponse> rebuildFreeipaWithHttpInfo(@jakarta.annotation.Nonnull RebuildFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling rebuildFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<RebuildFreeipaResponse> localVarReturnType = new GenericType<RebuildFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.rebuildFreeipa", "/api/v1/environments2/rebuildFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Repairs the FreeIPA nodes.
   * Repairs the FreeIPA nodes if they are in a non working state.
   * @param input  (required)
   * @return RepairFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public RepairFreeipaResponse repairFreeipa(@jakarta.annotation.Nonnull RepairFreeipaRequest input) throws ApiException {
    return repairFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Repairs the FreeIPA nodes.
   * Repairs the FreeIPA nodes if they are in a non working state.
   * @param input  (required)
   * @return ApiResponse&lt;RepairFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<RepairFreeipaResponse> repairFreeipaWithHttpInfo(@jakarta.annotation.Nonnull RepairFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling repairFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<RepairFreeipaResponse> localVarReturnType = new GenericType<RepairFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.repairFreeipa", "/api/v1/environments2/repairFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Retries the last failed operation on a FreeIPA.
   * Retries the last failed operation on a FreeIPA.
   * @param input  (required)
   * @return RetryFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public RetryFreeipaResponse retryFreeipa(@jakarta.annotation.Nonnull RetryFreeipaRequest input) throws ApiException {
    return retryFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Retries the last failed operation on a FreeIPA.
   * Retries the last failed operation on a FreeIPA.
   * @param input  (required)
   * @return ApiResponse&lt;RetryFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<RetryFreeipaResponse> retryFreeipaWithHttpInfo(@jakarta.annotation.Nonnull RetryFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling retryFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<RetryFreeipaResponse> localVarReturnType = new GenericType<RetryFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.retryFreeipa", "/api/v1/environments2/retryFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Rotate FreeIPA related secret for FreeIPA instances.
   * Rotate FreeIPA related secrets for FreeIPA instances.
   * @param input  (required)
   * @return RotateFreeipaSecretsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public RotateFreeipaSecretsResponse rotateFreeipaSecrets(@jakarta.annotation.Nonnull RotateFreeipaSecretsRequest input) throws ApiException {
    return rotateFreeipaSecretsWithHttpInfo(input).getData();
  }

  /**
   * Rotate FreeIPA related secret for FreeIPA instances.
   * Rotate FreeIPA related secrets for FreeIPA instances.
   * @param input  (required)
   * @return ApiResponse&lt;RotateFreeipaSecretsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<RotateFreeipaSecretsResponse> rotateFreeipaSecretsWithHttpInfo(@jakarta.annotation.Nonnull RotateFreeipaSecretsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling rotateFreeipaSecrets");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<RotateFreeipaSecretsResponse> localVarReturnType = new GenericType<RotateFreeipaSecretsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.rotateFreeipaSecrets", "/api/v1/environments2/rotateFreeipaSecrets", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Rotate SaltStack user password on FreeIPA instances.
   * Deprecated, please use rotateFreeipaSecrets with SALT_PASSWORD secretType instead.
   * @param input  (required)
   * @return RotateSaltPasswordResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public RotateSaltPasswordResponse rotateSaltPassword(@jakarta.annotation.Nonnull RotateSaltPasswordRequest input) throws ApiException {
    return rotateSaltPasswordWithHttpInfo(input).getData();
  }

  /**
   * Rotate SaltStack user password on FreeIPA instances.
   * Deprecated, please use rotateFreeipaSecrets with SALT_PASSWORD secretType instead.
   * @param input  (required)
   * @return ApiResponse&lt;RotateSaltPasswordResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<RotateSaltPasswordResponse> rotateSaltPasswordWithHttpInfo(@jakarta.annotation.Nonnull RotateSaltPasswordRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling rotateSaltPassword");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<RotateSaltPasswordResponse> localVarReturnType = new GenericType<RotateSaltPasswordResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.rotateSaltPassword", "/api/v1/environments2/rotateSaltPassword", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates or updates the AWS audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the AWS audit credential for the account. The response will only contain details appropriate to AWS.
   * @param input  (required)
   * @return SetAWSAuditCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetAWSAuditCredentialResponse setAWSAuditCredential(@jakarta.annotation.Nonnull SetAWSAuditCredentialRequest input) throws ApiException {
    return setAWSAuditCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates or updates the AWS audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the AWS audit credential for the account. The response will only contain details appropriate to AWS.
   * @param input  (required)
   * @return ApiResponse&lt;SetAWSAuditCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetAWSAuditCredentialResponse> setAWSAuditCredentialWithHttpInfo(@jakarta.annotation.Nonnull SetAWSAuditCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setAWSAuditCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetAWSAuditCredentialResponse> localVarReturnType = new GenericType<SetAWSAuditCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setAWSAuditCredential", "/api/v1/environments2/setAWSAuditCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates or updates the AWS GovCloud audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the AWS GovCloud audit credential for the account. The response will only contain details appropriate to AWS.
   * @param input  (required)
   * @return SetAWSGovCloudAuditCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetAWSGovCloudAuditCredentialResponse setAWSGovCloudAuditCredential(@jakarta.annotation.Nonnull SetAWSGovCloudAuditCredentialRequest input) throws ApiException {
    return setAWSGovCloudAuditCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates or updates the AWS GovCloud audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the AWS GovCloud audit credential for the account. The response will only contain details appropriate to AWS.
   * @param input  (required)
   * @return ApiResponse&lt;SetAWSGovCloudAuditCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetAWSGovCloudAuditCredentialResponse> setAWSGovCloudAuditCredentialWithHttpInfo(@jakarta.annotation.Nonnull SetAWSGovCloudAuditCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setAWSGovCloudAuditCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetAWSGovCloudAuditCredentialResponse> localVarReturnType = new GenericType<SetAWSGovCloudAuditCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setAWSGovCloudAuditCredential", "/api/v1/environments2/setAWSGovCloudAuditCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Set account level telemetry settings. (telemetry features and anonymization rules)
   * Set account level telemetry settings. (telemetry features and anonymization rules)
   * @param input  (required)
   * @return SetAccountTelemetryResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetAccountTelemetryResponse setAccountTelemetry(@jakarta.annotation.Nonnull SetAccountTelemetryRequest input) throws ApiException {
    return setAccountTelemetryWithHttpInfo(input).getData();
  }

  /**
   * Set account level telemetry settings. (telemetry features and anonymization rules)
   * Set account level telemetry settings. (telemetry features and anonymization rules)
   * @param input  (required)
   * @return ApiResponse&lt;SetAccountTelemetryResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetAccountTelemetryResponse> setAccountTelemetryWithHttpInfo(@jakarta.annotation.Nonnull SetAccountTelemetryRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setAccountTelemetry");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetAccountTelemetryResponse> localVarReturnType = new GenericType<SetAccountTelemetryResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setAccountTelemetry", "/api/v1/environments2/setAccountTelemetry", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates or updates the Azure audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the Azure audit credential for the account. The response will only contain details appropriate to Azure.
   * @param input  (required)
   * @return SetAzureAuditCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetAzureAuditCredentialResponse setAzureAuditCredential(@jakarta.annotation.Nonnull SetAzureAuditCredentialRequest input) throws ApiException {
    return setAzureAuditCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates or updates the Azure audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the Azure audit credential for the account. The response will only contain details appropriate to Azure.
   * @param input  (required)
   * @return ApiResponse&lt;SetAzureAuditCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetAzureAuditCredentialResponse> setAzureAuditCredentialWithHttpInfo(@jakarta.annotation.Nonnull SetAzureAuditCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setAzureAuditCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetAzureAuditCredentialResponse> localVarReturnType = new GenericType<SetAzureAuditCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setAzureAuditCredential", "/api/v1/environments2/setAzureAuditCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Sets a catalog for a FreeIPA installation.
   * Sets a catalog for a FreeIPA installation.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object setCatalog(@jakarta.annotation.Nonnull SetCatalogRequest input) throws ApiException {
    return setCatalogWithHttpInfo(input).getData();
  }

  /**
   * Sets a catalog for a FreeIPA installation.
   * Sets a catalog for a FreeIPA installation.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> setCatalogWithHttpInfo(@jakarta.annotation.Nonnull SetCatalogRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setCatalog");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setCatalog", "/api/v1/environments2/setCatalog", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Sets endpoint access gateway settings for the environment.
   * Sets endpoint access gateway settings for the environment.
   * @param input  (required)
   * @return SetEndpointAccessGatewayResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetEndpointAccessGatewayResponse setEndpointAccessGateway(@jakarta.annotation.Nonnull SetEndpointAccessGatewayRequest input) throws ApiException {
    return setEndpointAccessGatewayWithHttpInfo(input).getData();
  }

  /**
   * Sets endpoint access gateway settings for the environment.
   * Sets endpoint access gateway settings for the environment.
   * @param input  (required)
   * @return ApiResponse&lt;SetEndpointAccessGatewayResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetEndpointAccessGatewayResponse> setEndpointAccessGatewayWithHttpInfo(@jakarta.annotation.Nonnull SetEndpointAccessGatewayRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setEndpointAccessGateway");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetEndpointAccessGatewayResponse> localVarReturnType = new GenericType<SetEndpointAccessGatewayResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setEndpointAccessGateway", "/api/v1/environments2/setEndpointAccessGateway", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Read a configuration setting from the environment service.
   * Read a configuration setting from the environment service.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object setEnvironmentSetting(@jakarta.annotation.Nonnull SetEnvironmentSettingRequest input) throws ApiException {
    return setEnvironmentSettingWithHttpInfo(input).getData();
  }

  /**
   * Read a configuration setting from the environment service.
   * Read a configuration setting from the environment service.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> setEnvironmentSettingWithHttpInfo(@jakarta.annotation.Nonnull SetEnvironmentSettingRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setEnvironmentSetting");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setEnvironmentSetting", "/api/v1/environments2/setEnvironmentSetting", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Creates or updates the GCP audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the GCP audit credential for the account. The response will only contain details appropriate to GCP.
   * @param input  (required)
   * @return SetGCPAuditCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetGCPAuditCredentialResponse setGCPAuditCredential(@jakarta.annotation.Nonnull SetGCPAuditCredentialRequest input) throws ApiException {
    return setGCPAuditCredentialWithHttpInfo(input).getData();
  }

  /**
   * Creates or updates the GCP audit credential for the account. The credential is used for authorization to archive audit events to your cloud storage.
   * Creates or updates the GCP audit credential for the account. The response will only contain details appropriate to GCP.
   * @param input  (required)
   * @return ApiResponse&lt;SetGCPAuditCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetGCPAuditCredentialResponse> setGCPAuditCredentialWithHttpInfo(@jakarta.annotation.Nonnull SetGCPAuditCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setGCPAuditCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetGCPAuditCredentialResponse> localVarReturnType = new GenericType<SetGCPAuditCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setGCPAuditCredential", "/api/v1/environments2/setGCPAuditCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Sets all ID Broker mappings for an environment.
   * Sets all ID Broker mappings for an environment. Overwrites all existing mappings.
   * @param input  (required)
   * @return SetIdBrokerMappingsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetIdBrokerMappingsResponse setIdBrokerMappings(@jakarta.annotation.Nonnull SetIdBrokerMappingsRequest input) throws ApiException {
    return setIdBrokerMappingsWithHttpInfo(input).getData();
  }

  /**
   * Sets all ID Broker mappings for an environment.
   * Sets all ID Broker mappings for an environment. Overwrites all existing mappings.
   * @param input  (required)
   * @return ApiResponse&lt;SetIdBrokerMappingsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetIdBrokerMappingsResponse> setIdBrokerMappingsWithHttpInfo(@jakarta.annotation.Nonnull SetIdBrokerMappingsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setIdBrokerMappings");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetIdBrokerMappingsResponse> localVarReturnType = new GenericType<SetIdBrokerMappingsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setIdBrokerMappings", "/api/v1/environments2/setIdBrokerMappings", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Sets workload user&#39;s password and updates into all environments.
   * Deprecated. Use &#39;iam set-workload-password&#39; command instead.
   * @param input  (required)
   * @return SetPasswordResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetPasswordResponse setPassword(@jakarta.annotation.Nonnull SetPasswordRequest input) throws ApiException {
    return setPasswordWithHttpInfo(input).getData();
  }

  /**
   * Sets workload user&#39;s password and updates into all environments.
   * Deprecated. Use &#39;iam set-workload-password&#39; command instead.
   * @param input  (required)
   * @return ApiResponse&lt;SetPasswordResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetPasswordResponse> setPasswordWithHttpInfo(@jakarta.annotation.Nonnull SetPasswordRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setPassword");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetPasswordResponse> localVarReturnType = new GenericType<SetPasswordResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setPassword", "/api/v1/environments2/setPassword", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Configure environment level telemetry feature setting.
   * Configure environment level telemetry feature setting.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object setTelemetryFeatures(@jakarta.annotation.Nonnull SetTelemetryFeaturesRequest input) throws ApiException {
    return setTelemetryFeaturesWithHttpInfo(input).getData();
  }

  /**
   * Configure environment level telemetry feature setting.
   * Configure environment level telemetry feature setting.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> setTelemetryFeaturesWithHttpInfo(@jakarta.annotation.Nonnull SetTelemetryFeaturesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setTelemetryFeatures");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setTelemetryFeatures", "/api/v1/environments2/setTelemetryFeatures", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Set up cross-realm trust for FreeIPA.
   * Initiates cross-realm trust setup between FreeIPA and Active Directory. After this operation completes, configure the Active Directory side of the trust relationship. Then call finishSetupTrust to validate and activate the trust for production use.
   * @param input  (required)
   * @return SetupTrustResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SetupTrustResponse setupTrust(@jakarta.annotation.Nonnull SetupTrustRequest input) throws ApiException {
    return setupTrustWithHttpInfo(input).getData();
  }

  /**
   * Set up cross-realm trust for FreeIPA.
   * Initiates cross-realm trust setup between FreeIPA and Active Directory. After this operation completes, configure the Active Directory side of the trust relationship. Then call finishSetupTrust to validate and activate the trust for production use.
   * @param input  (required)
   * @return ApiResponse&lt;SetupTrustResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SetupTrustResponse> setupTrustWithHttpInfo(@jakarta.annotation.Nonnull SetupTrustRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling setupTrust");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SetupTrustResponse> localVarReturnType = new GenericType<SetupTrustResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.setupTrust", "/api/v1/environments2/setupTrust", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Start an environment.
   * Start an environment.
   * @param input  (required)
   * @return StartEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public StartEnvironmentResponse startEnvironment(@jakarta.annotation.Nonnull StartEnvironmentRequest input) throws ApiException {
    return startEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Start an environment.
   * Start an environment.
   * @param input  (required)
   * @return ApiResponse&lt;StartEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<StartEnvironmentResponse> startEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull StartEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling startEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<StartEnvironmentResponse> localVarReturnType = new GenericType<StartEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.startEnvironment", "/api/v1/environments2/startEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Initiates the vertical scaling on FreeIPA.
   * Initiates the vertical scaling on FreeIPA.
   * @param input  (required)
   * @return StartFreeIpaVerticalScalingResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public StartFreeIpaVerticalScalingResponse startFreeIpaVerticalScaling(@jakarta.annotation.Nonnull StartFreeIpaVerticalScalingRequest input) throws ApiException {
    return startFreeIpaVerticalScalingWithHttpInfo(input).getData();
  }

  /**
   * Initiates the vertical scaling on FreeIPA.
   * Initiates the vertical scaling on FreeIPA.
   * @param input  (required)
   * @return ApiResponse&lt;StartFreeIpaVerticalScalingResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<StartFreeIpaVerticalScalingResponse> startFreeIpaVerticalScalingWithHttpInfo(@jakarta.annotation.Nonnull StartFreeIpaVerticalScalingRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling startFreeIpaVerticalScaling");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<StartFreeIpaVerticalScalingResponse> localVarReturnType = new GenericType<StartFreeIpaVerticalScalingResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.startFreeIpaVerticalScaling", "/api/v1/environments2/startFreeIpaVerticalScaling", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Stop an environment.
   * Stop an environment.
   * @param input  (required)
   * @return StopEnvironmentResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public StopEnvironmentResponse stopEnvironment(@jakarta.annotation.Nonnull StopEnvironmentRequest input) throws ApiException {
    return stopEnvironmentWithHttpInfo(input).getData();
  }

  /**
   * Stop an environment.
   * Stop an environment.
   * @param input  (required)
   * @return ApiResponse&lt;StopEnvironmentResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<StopEnvironmentResponse> stopEnvironmentWithHttpInfo(@jakarta.annotation.Nonnull StopEnvironmentRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling stopEnvironment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<StopEnvironmentResponse> localVarReturnType = new GenericType<StopEnvironmentResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.stopEnvironment", "/api/v1/environments2/stopEnvironment", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Synchronizes environments with all users and groups state with CDP.
   * Synchronizes environments with all users and groups state with CDP.
   * @param input  (required)
   * @return SyncAllUsersResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SyncAllUsersResponse syncAllUsers(@jakarta.annotation.Nonnull SyncAllUsersRequest input) throws ApiException {
    return syncAllUsersWithHttpInfo(input).getData();
  }

  /**
   * Synchronizes environments with all users and groups state with CDP.
   * Synchronizes environments with all users and groups state with CDP.
   * @param input  (required)
   * @return ApiResponse&lt;SyncAllUsersResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SyncAllUsersResponse> syncAllUsersWithHttpInfo(@jakarta.annotation.Nonnull SyncAllUsersRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling syncAllUsers");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SyncAllUsersResponse> localVarReturnType = new GenericType<SyncAllUsersResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.syncAllUsers", "/api/v1/environments2/syncAllUsers", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Syncs all ID Broker mappings for an environment.
   * Syncs all ID Broker mappings to all datalake clusters in an environment.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object syncIdBrokerMappings(@jakarta.annotation.Nonnull SyncIdBrokerMappingsRequest input) throws ApiException {
    return syncIdBrokerMappingsWithHttpInfo(input).getData();
  }

  /**
   * Syncs all ID Broker mappings for an environment.
   * Syncs all ID Broker mappings to all datalake clusters in an environment.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> syncIdBrokerMappingsWithHttpInfo(@jakarta.annotation.Nonnull SyncIdBrokerMappingsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling syncIdBrokerMappings");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.syncIdBrokerMappings", "/api/v1/environments2/syncIdBrokerMappings", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Returns status of the sync operation for the operation id.
   * Returns status of the sync operation for the operation id. Operation Id should be one of the previously request sync operation.
   * @param input  (required)
   * @return SyncStatusResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SyncStatusResponse syncStatus(@jakarta.annotation.Nonnull SyncStatusRequest input) throws ApiException {
    return syncStatusWithHttpInfo(input).getData();
  }

  /**
   * Returns status of the sync operation for the operation id.
   * Returns status of the sync operation for the operation id. Operation Id should be one of the previously request sync operation.
   * @param input  (required)
   * @return ApiResponse&lt;SyncStatusResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SyncStatusResponse> syncStatusWithHttpInfo(@jakarta.annotation.Nonnull SyncStatusRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling syncStatus");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SyncStatusResponse> localVarReturnType = new GenericType<SyncStatusResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.syncStatus", "/api/v1/environments2/syncStatus", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Synchronizes environments with single user to the FreeIPA servers.
   * Synchronizes environments with single user to the FreeIPA servers.
   * @param input  (required)
   * @return SyncUserResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public SyncUserResponse syncUser(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return syncUserWithHttpInfo(input).getData();
  }

  /**
   * Synchronizes environments with single user to the FreeIPA servers.
   * Synchronizes environments with single user to the FreeIPA servers.
   * @param input  (required)
   * @return ApiResponse&lt;SyncUserResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<SyncUserResponse> syncUserWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling syncUser");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<SyncUserResponse> localVarReturnType = new GenericType<SyncUserResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.syncUser", "/api/v1/environments2/syncUser", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Synchronizes all environments in the calling account.
   * Deprecated. No plans to release this API.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object synchronizeAllEnvironments(@jakarta.annotation.Nonnull Object input) throws ApiException {
    return synchronizeAllEnvironmentsWithHttpInfo(input).getData();
  }

  /**
   * Synchronizes all environments in the calling account.
   * Deprecated. No plans to release this API.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> synchronizeAllEnvironmentsWithHttpInfo(@jakarta.annotation.Nonnull Object input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling synchronizeAllEnvironments");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.synchronizeAllEnvironments", "/api/v1/environments2/synchronizeAllEnvironments", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Test anonymization rules (for account telemetry) against text input.
   * Test anonymization rules (for account telemetry) against text input.
   * @param input  (required)
   * @return TestAccountTelemetryRulesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public TestAccountTelemetryRulesResponse testAccountTelemetryRules(@jakarta.annotation.Nonnull TestAccountTelemetryRulesRequest input) throws ApiException {
    return testAccountTelemetryRulesWithHttpInfo(input).getData();
  }

  /**
   * Test anonymization rules (for account telemetry) against text input.
   * Test anonymization rules (for account telemetry) against text input.
   * @param input  (required)
   * @return ApiResponse&lt;TestAccountTelemetryRulesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TestAccountTelemetryRulesResponse> testAccountTelemetryRulesWithHttpInfo(@jakarta.annotation.Nonnull TestAccountTelemetryRulesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling testAccountTelemetryRules");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<TestAccountTelemetryRulesResponse> localVarReturnType = new GenericType<TestAccountTelemetryRulesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.testAccountTelemetryRules", "/api/v1/environments2/testAccountTelemetryRules", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates an AWS credential that can be attached to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Updates an AWS credential.
   * @param input  (required)
   * @return UpdateAwsCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateAwsCredentialResponse updateAwsCredential(@jakarta.annotation.Nonnull UpdateAwsCredentialRequest input) throws ApiException {
    return updateAwsCredentialWithHttpInfo(input).getData();
  }

  /**
   * Updates an AWS credential that can be attached to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Updates an AWS credential.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateAwsCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateAwsCredentialResponse> updateAwsCredentialWithHttpInfo(@jakarta.annotation.Nonnull UpdateAwsCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAwsCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateAwsCredentialResponse> localVarReturnType = new GenericType<UpdateAwsCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAwsCredential", "/api/v1/environments2/updateAwsCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update the AWS encryption key ARN for the environment.
   * Updates the AWS encryption key ARN for the environment. Enables the server side encryption with CMK for newly created AWS resources for the existing environment.
   * @param input  (required)
   * @return UpdateAwsDiskEncryptionParametersResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateAwsDiskEncryptionParametersResponse updateAwsDiskEncryptionParameters(@jakarta.annotation.Nonnull UpdateAwsDiskEncryptionParametersRequest input) throws ApiException {
    return updateAwsDiskEncryptionParametersWithHttpInfo(input).getData();
  }

  /**
   * Update the AWS encryption key ARN for the environment.
   * Updates the AWS encryption key ARN for the environment. Enables the server side encryption with CMK for newly created AWS resources for the existing environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateAwsDiskEncryptionParametersResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateAwsDiskEncryptionParametersResponse> updateAwsDiskEncryptionParametersWithHttpInfo(@jakarta.annotation.Nonnull UpdateAwsDiskEncryptionParametersRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAwsDiskEncryptionParameters");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateAwsDiskEncryptionParametersResponse> localVarReturnType = new GenericType<UpdateAwsDiskEncryptionParametersResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAwsDiskEncryptionParameters", "/api/v1/environments2/updateAwsDiskEncryptionParameters", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update the Availability Zones for the Azure environment.
   * Updates the Availability Zones for the Azure environment.
   * @param input  (required)
   * @return UpdateAzureAvailabilityZonesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateAzureAvailabilityZonesResponse updateAzureAvailabilityZones(@jakarta.annotation.Nonnull UpdateAzureAvailabilityZonesRequest input) throws ApiException {
    return updateAzureAvailabilityZonesWithHttpInfo(input).getData();
  }

  /**
   * Update the Availability Zones for the Azure environment.
   * Updates the Availability Zones for the Azure environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateAzureAvailabilityZonesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateAzureAvailabilityZonesResponse> updateAzureAvailabilityZonesWithHttpInfo(@jakarta.annotation.Nonnull UpdateAzureAvailabilityZonesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAzureAvailabilityZones");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateAzureAvailabilityZonesResponse> localVarReturnType = new GenericType<UpdateAzureAvailabilityZonesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAzureAvailabilityZones", "/api/v1/environments2/updateAzureAvailabilityZones", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates a new certificate based Azure credential that can be attached to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Updates a certificate based Azure credential.
   * @param input  (required)
   * @return UpdateAzureCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateAzureCredentialResponse updateAzureCredential(@jakarta.annotation.Nonnull UpdateAzureCredentialRequest input) throws ApiException {
    return updateAzureCredentialWithHttpInfo(input).getData();
  }

  /**
   * Updates a new certificate based Azure credential that can be attached to an environment. The credential is used for authorization to provision resources such as compute instances within your cloud provider account.
   * Updates a certificate based Azure credential.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateAzureCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateAzureCredentialResponse> updateAzureCredentialWithHttpInfo(@jakarta.annotation.Nonnull UpdateAzureCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAzureCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateAzureCredentialResponse> localVarReturnType = new GenericType<UpdateAzureCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAzureCredential", "/api/v1/environments2/updateAzureCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update the Azure database resources for the environment.
   * Updates the Azure database resources for the environment. Enables deploying private Flexible Server for the existing environment.
   * @param input  (required)
   * @return UpdateAzureDatabaseResourcesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateAzureDatabaseResourcesResponse updateAzureDatabaseResources(@jakarta.annotation.Nonnull UpdateAzureDatabaseResourcesRequest input) throws ApiException {
    return updateAzureDatabaseResourcesWithHttpInfo(input).getData();
  }

  /**
   * Update the Azure database resources for the environment.
   * Updates the Azure database resources for the environment. Enables deploying private Flexible Server for the existing environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateAzureDatabaseResourcesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateAzureDatabaseResourcesResponse> updateAzureDatabaseResourcesWithHttpInfo(@jakarta.annotation.Nonnull UpdateAzureDatabaseResourcesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAzureDatabaseResources");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateAzureDatabaseResourcesResponse> localVarReturnType = new GenericType<UpdateAzureDatabaseResourcesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAzureDatabaseResources", "/api/v1/environments2/updateAzureDatabaseResources", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update the Azure encryption resources for the environment.
   * Updates the Azure encryption resources for the environment. Enables the SSE with CMK for newly created Azure resources for the existing environment.
   * @param input  (required)
   * @return UpdateAzureEncryptionResourcesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateAzureEncryptionResourcesResponse updateAzureEncryptionResources(@jakarta.annotation.Nonnull UpdateAzureEncryptionResourcesRequest input) throws ApiException {
    return updateAzureEncryptionResourcesWithHttpInfo(input).getData();
  }

  /**
   * Update the Azure encryption resources for the environment.
   * Updates the Azure encryption resources for the environment. Enables the SSE with CMK for newly created Azure resources for the existing environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateAzureEncryptionResourcesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateAzureEncryptionResourcesResponse> updateAzureEncryptionResourcesWithHttpInfo(@jakarta.annotation.Nonnull UpdateAzureEncryptionResourcesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAzureEncryptionResources");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateAzureEncryptionResourcesResponse> localVarReturnType = new GenericType<UpdateAzureEncryptionResourcesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAzureEncryptionResources", "/api/v1/environments2/updateAzureEncryptionResources", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * Updates account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object updateAzureImageTermsPolicy(@jakarta.annotation.Nonnull UpdateAzureImageTermsPolicyRequest input) throws ApiException {
    return updateAzureImageTermsPolicyWithHttpInfo(input).getData();
  }

  /**
   * Updates account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * Updates account level Azure Marketplace image policy. CDP is capable to automatically accept Azure Marketplace image terms during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> updateAzureImageTermsPolicyWithHttpInfo(@jakarta.annotation.Nonnull UpdateAzureImageTermsPolicyRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateAzureImageTermsPolicy");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateAzureImageTermsPolicy", "/api/v1/environments2/updateAzureImageTermsPolicy", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates custom docker registry CRN of an environment.
   * Updates custom docker registry CRN of an environment.
   * @param input  (required)
   * @return UpdateCustomDockerRegistryResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateCustomDockerRegistryResponse updateCustomDockerRegistry(@jakarta.annotation.Nonnull UpdateCustomDockerRegistryRequest input) throws ApiException {
    return updateCustomDockerRegistryWithHttpInfo(input).getData();
  }

  /**
   * Updates custom docker registry CRN of an environment.
   * Updates custom docker registry CRN of an environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateCustomDockerRegistryResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateCustomDockerRegistryResponse> updateCustomDockerRegistryWithHttpInfo(@jakarta.annotation.Nonnull UpdateCustomDockerRegistryRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateCustomDockerRegistry");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateCustomDockerRegistryResponse> localVarReturnType = new GenericType<UpdateCustomDockerRegistryResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateCustomDockerRegistry", "/api/v1/environments2/updateCustomDockerRegistry", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates Data Service resources of an environment.
   * Updates Data Service resources of an environment.
   * @param input  (required)
   * @return UpdateDataServiceResourcesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateDataServiceResourcesResponse updateDataServiceResources(@jakarta.annotation.Nonnull UpdateDataServiceResourcesRequest input) throws ApiException {
    return updateDataServiceResourcesWithHttpInfo(input).getData();
  }

  /**
   * Updates Data Service resources of an environment.
   * Updates Data Service resources of an environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateDataServiceResourcesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateDataServiceResourcesResponse> updateDataServiceResourcesWithHttpInfo(@jakarta.annotation.Nonnull UpdateDataServiceResourcesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateDataServiceResources");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateDataServiceResourcesResponse> localVarReturnType = new GenericType<UpdateDataServiceResourcesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateDataServiceResources", "/api/v1/environments2/updateDataServiceResources", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates FreeIPA AWS cluster to use IMDSv1.
   * Updates FreeIPA AWS cluster to use IMDSv1.
   * @param input  (required)
   * @return UpdateFreeipaToAwsImdsV1Response
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateFreeipaToAwsImdsV1Response updateFreeipaToAwsImdsV1(@jakarta.annotation.Nonnull UpdateFreeipaToAwsImdsV1Request input) throws ApiException {
    return updateFreeipaToAwsImdsV1WithHttpInfo(input).getData();
  }

  /**
   * Updates FreeIPA AWS cluster to use IMDSv1.
   * Updates FreeIPA AWS cluster to use IMDSv1.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateFreeipaToAwsImdsV1Response&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateFreeipaToAwsImdsV1Response> updateFreeipaToAwsImdsV1WithHttpInfo(@jakarta.annotation.Nonnull UpdateFreeipaToAwsImdsV1Request input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateFreeipaToAwsImdsV1");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateFreeipaToAwsImdsV1Response> localVarReturnType = new GenericType<UpdateFreeipaToAwsImdsV1Response>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateFreeipaToAwsImdsV1", "/api/v1/environments2/updateFreeipaToAwsImdsV1", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates FreeIPA AWS cluster to use IMDSv2.
   * Updates FreeIPA AWS cluster to use IMDSv2.
   * @param input  (required)
   * @return UpdateFreeipaToAwsImdsV2Response
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateFreeipaToAwsImdsV2Response updateFreeipaToAwsImdsV2(@jakarta.annotation.Nonnull UpdateFreeipaToAwsImdsV2Request input) throws ApiException {
    return updateFreeipaToAwsImdsV2WithHttpInfo(input).getData();
  }

  /**
   * Updates FreeIPA AWS cluster to use IMDSv2.
   * Updates FreeIPA AWS cluster to use IMDSv2.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateFreeipaToAwsImdsV2Response&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateFreeipaToAwsImdsV2Response> updateFreeipaToAwsImdsV2WithHttpInfo(@jakarta.annotation.Nonnull UpdateFreeipaToAwsImdsV2Request input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateFreeipaToAwsImdsV2");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateFreeipaToAwsImdsV2Response> localVarReturnType = new GenericType<UpdateFreeipaToAwsImdsV2Response>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateFreeipaToAwsImdsV2", "/api/v1/environments2/updateFreeipaToAwsImdsV2", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates an existing GCP credential.
   * Updates an existing GCP credential.
   * @param input  (required)
   * @return UpdateGCPCredentialResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateGCPCredentialResponse updateGCPCredential(@jakarta.annotation.Nonnull UpdateGCPCredentialRequest input) throws ApiException {
    return updateGCPCredentialWithHttpInfo(input).getData();
  }

  /**
   * Updates an existing GCP credential.
   * Updates an existing GCP credential.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateGCPCredentialResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateGCPCredentialResponse> updateGCPCredentialWithHttpInfo(@jakarta.annotation.Nonnull UpdateGCPCredentialRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateGCPCredential");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateGCPCredentialResponse> localVarReturnType = new GenericType<UpdateGCPCredentialResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateGCPCredential", "/api/v1/environments2/updateGCPCredential", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update the Availability Zones for the GCP environment.
   * Updates the Availability Zones for the GCP environment.
   * @param input  (required)
   * @return UpdateGcpAvailabilityZonesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateGcpAvailabilityZonesResponse updateGcpAvailabilityZones(@jakarta.annotation.Nonnull UpdateGcpAvailabilityZonesRequest input) throws ApiException {
    return updateGcpAvailabilityZonesWithHttpInfo(input).getData();
  }

  /**
   * Update the Availability Zones for the GCP environment.
   * Updates the Availability Zones for the GCP environment.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateGcpAvailabilityZonesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateGcpAvailabilityZonesResponse> updateGcpAvailabilityZonesWithHttpInfo(@jakarta.annotation.Nonnull UpdateGcpAvailabilityZonesRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateGcpAvailabilityZones");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateGcpAvailabilityZonesResponse> localVarReturnType = new GenericType<UpdateGcpAvailabilityZonesResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateGcpAvailabilityZones", "/api/v1/environments2/updateGcpAvailabilityZones", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Run orchestrator engine state update on the FreeIPA cluster.
   * Run orchestrator engine state update on the FreeIPA cluster.
   * @param input  (required)
   * @return UpdateOrchestratorStateResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateOrchestratorStateResponse updateOrchestratorState(@jakarta.annotation.Nonnull UpdateOrchestratorStateRequest input) throws ApiException {
    return updateOrchestratorStateWithHttpInfo(input).getData();
  }

  /**
   * Run orchestrator engine state update on the FreeIPA cluster.
   * Run orchestrator engine state update on the FreeIPA cluster.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateOrchestratorStateResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateOrchestratorStateResponse> updateOrchestratorStateWithHttpInfo(@jakarta.annotation.Nonnull UpdateOrchestratorStateRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateOrchestratorState");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateOrchestratorStateResponse> localVarReturnType = new GenericType<UpdateOrchestratorStateResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateOrchestratorState", "/api/v1/environments2/updateOrchestratorState", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates the proxy config of the given environment. The newly added proxy configs will only be used for the Cloudera Data Hub clusters created within the environment after the new proxy configs were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the proxy configs originally defined during environment creation.
   * Updates the proxy config of the given environment. The newly added proxy configs will only be used for the Cloudera Data Hub clusters created within the environment after the new proxy configs were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the proxy configs originally defined during environment creation.
   * @param input  (required)
   * @return Object
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public Object updateProxyConfig(@jakarta.annotation.Nonnull UpdateProxyConfigRequest input) throws ApiException {
    return updateProxyConfigWithHttpInfo(input).getData();
  }

  /**
   * Updates the proxy config of the given environment. The newly added proxy configs will only be used for the Cloudera Data Hub clusters created within the environment after the new proxy configs were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the proxy configs originally defined during environment creation.
   * Updates the proxy config of the given environment. The newly added proxy configs will only be used for the Cloudera Data Hub clusters created within the environment after the new proxy configs were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the proxy configs originally defined during environment creation.
   * @param input  (required)
   * @return ApiResponse&lt;Object&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Object> updateProxyConfigWithHttpInfo(@jakarta.annotation.Nonnull UpdateProxyConfigRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateProxyConfig");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateProxyConfig", "/api/v1/environments2/updateProxyConfig", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update root volume size and type in FreeIPA instances.
   * This updates the root volume size and type for the FreeIPA instances.
   * @param input  (required)
   * @return UpdateRootVolumeFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateRootVolumeFreeipaResponse updateRootVolumeFreeipa(@jakarta.annotation.Nonnull UpdateRootVolumeFreeipaRequest input) throws ApiException {
    return updateRootVolumeFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Update root volume size and type in FreeIPA instances.
   * This updates the root volume size and type for the FreeIPA instances.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateRootVolumeFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateRootVolumeFreeipaResponse> updateRootVolumeFreeipaWithHttpInfo(@jakarta.annotation.Nonnull UpdateRootVolumeFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateRootVolumeFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateRootVolumeFreeipaResponse> localVarReturnType = new GenericType<UpdateRootVolumeFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateRootVolumeFreeipa", "/api/v1/environments2/updateRootVolumeFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates the security access settings of the given environment. The newly added security groups will only be used for the Cloudera Data Hub clusters created within the environment after the new security groups were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the security groups originally defined during environment creation.
   * Updates the security access settings of the given environment. The newly added security groups will only be used for the Cloudera Data Hub clusters created within the environment after the new security groups were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the security groups originally defined during environment creation.
   * @param input  (required)
   * @return UpdateSecurityAccessResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateSecurityAccessResponse updateSecurityAccess(@jakarta.annotation.Nonnull UpdateSecurityAccessRequest input) throws ApiException {
    return updateSecurityAccessWithHttpInfo(input).getData();
  }

  /**
   * Updates the security access settings of the given environment. The newly added security groups will only be used for the Cloudera Data Hub clusters created within the environment after the new security groups were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the security groups originally defined during environment creation.
   * Updates the security access settings of the given environment. The newly added security groups will only be used for the Cloudera Data Hub clusters created within the environment after the new security groups were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the security groups originally defined during environment creation.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateSecurityAccessResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateSecurityAccessResponse> updateSecurityAccessWithHttpInfo(@jakarta.annotation.Nonnull UpdateSecurityAccessRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateSecurityAccess");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateSecurityAccessResponse> localVarReturnType = new GenericType<UpdateSecurityAccessResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateSecurityAccess", "/api/v1/environments2/updateSecurityAccess", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates the designated SSH key for the given environment. The newly added SSH key will only be used for the Cloudera Data Hub clusters created within the environment after the new SSH key were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the SSH key originally defined during environment creation.
   * Updates the designated SSH key for the given environment. The newly added SSH key will only be used for the Cloudera Data Hub clusters created within the environment after the new SSH key were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the SSH key originally defined during environment creation.
   * @param input  (required)
   * @return UpdateSshKeyResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateSshKeyResponse updateSshKey(@jakarta.annotation.Nonnull UpdateSshKeyRequest input) throws ApiException {
    return updateSshKeyWithHttpInfo(input).getData();
  }

  /**
   * Updates the designated SSH key for the given environment. The newly added SSH key will only be used for the Cloudera Data Hub clusters created within the environment after the new SSH key were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the SSH key originally defined during environment creation.
   * Updates the designated SSH key for the given environment. The newly added SSH key will only be used for the Cloudera Data Hub clusters created within the environment after the new SSH key were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the SSH key originally defined during environment creation.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateSshKeyResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateSshKeyResponse> updateSshKeyWithHttpInfo(@jakarta.annotation.Nonnull UpdateSshKeyRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateSshKey");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateSshKeyResponse> localVarReturnType = new GenericType<UpdateSshKeyResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateSshKey", "/api/v1/environments2/updateSshKey", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates the subnet(s) of the given environment. The newly added subnets will only be used for the Cloudera Data Hub clusters created within the environment after the new subnets were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the subnets originally defined during environment creation.
   * Updates the subnet(s) of the given environment. The newly added subnets will only be used for the Cloudera Data Hub clusters created within the environment after the new subnets were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the subnets originally defined during environment creation.
   * @param input  (required)
   * @return UpdateSubnetResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateSubnetResponse updateSubnet(@jakarta.annotation.Nonnull UpdateSubnetRequest input) throws ApiException {
    return updateSubnetWithHttpInfo(input).getData();
  }

  /**
   * Updates the subnet(s) of the given environment. The newly added subnets will only be used for the Cloudera Data Hub clusters created within the environment after the new subnets were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the subnets originally defined during environment creation.
   * Updates the subnet(s) of the given environment. The newly added subnets will only be used for the Cloudera Data Hub clusters created within the environment after the new subnets were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain within the subnets originally defined during environment creation.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateSubnetResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateSubnetResponse> updateSubnetWithHttpInfo(@jakarta.annotation.Nonnull UpdateSubnetRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateSubnet");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateSubnetResponse> localVarReturnType = new GenericType<UpdateSubnetResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateSubnet", "/api/v1/environments2/updateSubnet", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Updates the user defined tags of the given environment. The newly added tags will only be used for the Cloudera Data Hub clusters created within the environment after the new tags were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the tags originally defined during environment creation.
   * Updates the user defined tags of the given environment. The newly added tags will only be used for the Cloudera Data Hub clusters created within the environment after the new tags were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the tags originally defined during environment creation.
   * @param input  (required)
   * @return UpdateTagsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpdateTagsResponse updateTags(@jakarta.annotation.Nonnull UpdateTagsRequest input) throws ApiException {
    return updateTagsWithHttpInfo(input).getData();
  }

  /**
   * Updates the user defined tags of the given environment. The newly added tags will only be used for the Cloudera Data Hub clusters created within the environment after the new tags were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the tags originally defined during environment creation.
   * Updates the user defined tags of the given environment. The newly added tags will only be used for the Cloudera Data Hub clusters created within the environment after the new tags were added. All the existing environment resources such as the Data Lake, FreeIPA, and any existing Cloudera Data Hub clusters will remain with the tags originally defined during environment creation.
   * @param input  (required)
   * @return ApiResponse&lt;UpdateTagsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpdateTagsResponse> updateTagsWithHttpInfo(@jakarta.annotation.Nonnull UpdateTagsRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling updateTags");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpdateTagsResponse> localVarReturnType = new GenericType<UpdateTagsResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.updateTags", "/api/v1/environments2/updateTags", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Upgrades Cluster Connectivity Manager on the environment to the latest available version.
   * Upgrades Cluster Connectivity Manager on the environment to the latest available version.
   * @param input  (required)
   * @return UpgradeCcmResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpgradeCcmResponse upgradeCcm(@jakarta.annotation.Nonnull UpgradeCcmRequest input) throws ApiException {
    return upgradeCcmWithHttpInfo(input).getData();
  }

  /**
   * Upgrades Cluster Connectivity Manager on the environment to the latest available version.
   * Upgrades Cluster Connectivity Manager on the environment to the latest available version.
   * @param input  (required)
   * @return ApiResponse&lt;UpgradeCcmResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpgradeCcmResponse> upgradeCcmWithHttpInfo(@jakarta.annotation.Nonnull UpgradeCcmRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling upgradeCcm");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpgradeCcmResponse> localVarReturnType = new GenericType<UpgradeCcmResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.upgradeCcm", "/api/v1/environments2/upgradeCcm", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Upgrades FreeIPA to the latest or defined image.
   * FreeIPA is an integrated Identity and Authentication solution that can be used for any of CM, CDP services.
   * @param input  (required)
   * @return UpgradeFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpgradeFreeipaResponse upgradeFreeipa(@jakarta.annotation.Nonnull UpgradeFreeipaRequest input) throws ApiException {
    return upgradeFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Upgrades FreeIPA to the latest or defined image.
   * FreeIPA is an integrated Identity and Authentication solution that can be used for any of CM, CDP services.
   * @param input  (required)
   * @return ApiResponse&lt;UpgradeFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpgradeFreeipaResponse> upgradeFreeipaWithHttpInfo(@jakarta.annotation.Nonnull UpgradeFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling upgradeFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpgradeFreeipaResponse> localVarReturnType = new GenericType<UpgradeFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.upgradeFreeipa", "/api/v1/environments2/upgradeFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Upscales FreeIPA instances.
   * FreeIPA is an integrated Identity and Authentication solution that can be used for any of CM, CDP services.
   * @param input  (required)
   * @return UpscaleFreeipaResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public UpscaleFreeipaResponse upscaleFreeipa(@jakarta.annotation.Nonnull UpscaleFreeipaRequest input) throws ApiException {
    return upscaleFreeipaWithHttpInfo(input).getData();
  }

  /**
   * Upscales FreeIPA instances.
   * FreeIPA is an integrated Identity and Authentication solution that can be used for any of CM, CDP services.
   * @param input  (required)
   * @return ApiResponse&lt;UpscaleFreeipaResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<UpscaleFreeipaResponse> upscaleFreeipaWithHttpInfo(@jakarta.annotation.Nonnull UpscaleFreeipaRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling upscaleFreeipa");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<UpscaleFreeipaResponse> localVarReturnType = new GenericType<UpscaleFreeipaResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.upscaleFreeipa", "/api/v1/environments2/upscaleFreeipa", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Validates AWS cloud storage settings of the given environment.
   * Validates AWS cloud storage settings of the given environment.
   * @param input  (required)
   * @return ValidateAwsCloudStorageResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ValidateAwsCloudStorageResponse validateAwsCloudStorage(@jakarta.annotation.Nonnull ValidateAwsCloudStorageRequest input) throws ApiException {
    return validateAwsCloudStorageWithHttpInfo(input).getData();
  }

  /**
   * Validates AWS cloud storage settings of the given environment.
   * Validates AWS cloud storage settings of the given environment.
   * @param input  (required)
   * @return ApiResponse&lt;ValidateAwsCloudStorageResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ValidateAwsCloudStorageResponse> validateAwsCloudStorageWithHttpInfo(@jakarta.annotation.Nonnull ValidateAwsCloudStorageRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling validateAwsCloudStorage");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ValidateAwsCloudStorageResponse> localVarReturnType = new GenericType<ValidateAwsCloudStorageResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.validateAwsCloudStorage", "/api/v1/environments2/validateAwsCloudStorage", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Validates Azure cloud storage settings of the given environment.
   * Validates Azure cloud storage settings of the given environment.
   * @param input  (required)
   * @return ValidateAzureCloudStorageResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ValidateAzureCloudStorageResponse validateAzureCloudStorage(@jakarta.annotation.Nonnull ValidateAzureCloudStorageRequest input) throws ApiException {
    return validateAzureCloudStorageWithHttpInfo(input).getData();
  }

  /**
   * Validates Azure cloud storage settings of the given environment.
   * Validates Azure cloud storage settings of the given environment.
   * @param input  (required)
   * @return ApiResponse&lt;ValidateAzureCloudStorageResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Expected response to a valid request. </td><td>  -  </td></tr>
       <tr><td> 0 </td><td> The default response on an error. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ValidateAzureCloudStorageResponse> validateAzureCloudStorageWithHttpInfo(@jakarta.annotation.Nonnull ValidateAzureCloudStorageRequest input) throws ApiException {
    // Check required parameters
    if (input == null) {
      throw new ApiException(400, "Missing the required parameter 'input' when calling validateAzureCloudStorage");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ValidateAzureCloudStorageResponse> localVarReturnType = new GenericType<ValidateAzureCloudStorageResponse>() {};
    return apiClient.invokeAPI("EnvironmentPublicApi.validateAzureCloudStorage", "/api/v1/environments2/validateAzureCloudStorage", "POST", new ArrayList<>(), input,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
