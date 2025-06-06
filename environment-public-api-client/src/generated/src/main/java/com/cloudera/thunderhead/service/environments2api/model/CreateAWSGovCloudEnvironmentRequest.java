/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.util.Objects;
import java.util.Arrays;
import com.cloudera.thunderhead.service.environments2api.model.AWSFreeIpaCreationRequest;
import com.cloudera.thunderhead.service.environments2api.model.AuthenticationRequest;
import com.cloudera.thunderhead.service.environments2api.model.AwsLogStorageRequest;
import com.cloudera.thunderhead.service.environments2api.model.CcmV2TlsType;
import com.cloudera.thunderhead.service.environments2api.model.CustomDockerRegistryRequest;
import com.cloudera.thunderhead.service.environments2api.model.FreeIpaImageRequest;
import com.cloudera.thunderhead.service.environments2api.model.SecurityAccessRequest;
import com.cloudera.thunderhead.service.environments2api.model.SecurityRequest;
import com.cloudera.thunderhead.service.environments2api.model.TagRequest;
import com.cloudera.thunderhead.service.environments2api.model.TunnelType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Request object for a create AWS GovCloud environment request.
 */
@JsonPropertyOrder({
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ENVIRONMENT_NAME,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_CREDENTIAL_NAME,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_REGION,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_SECURITY_ACCESS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_AUTHENTICATION,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_LOG_STORAGE,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_VPC_ID,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_SUBNET_IDS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_NETWORK_CIDR,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_CREATE_PRIVATE_SUBNETS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_CREATE_SERVICE_ENDPOINTS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SCHEME,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_S3_GUARD_TABLE_NAME,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_DESCRIPTION,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ENABLE_TUNNEL,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ENABLE_WORKLOAD_ANALYTICS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_WORKLOAD_ANALYTICS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_TUNNEL_TYPE,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_CCM_V2_TLS_TYPE,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_CLOUD_STORAGE_LOGGING,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_FREE_IPA,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_SECURITY,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_IMAGE,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_TAGS,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_PROXY_CONFIG_NAME,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ENCRYPTION_KEY_ARN,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_ID_BROKER_MAPPING_SOURCE,
  CreateAWSGovCloudEnvironmentRequest.JSON_PROPERTY_CUSTOM_DOCKER_REGISTRY
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class CreateAWSGovCloudEnvironmentRequest {
  public static final String JSON_PROPERTY_ENVIRONMENT_NAME = "environmentName";
  private String environmentName;

  public static final String JSON_PROPERTY_CREDENTIAL_NAME = "credentialName";
  private String credentialName;

  public static final String JSON_PROPERTY_REGION = "region";
  private String region;

  public static final String JSON_PROPERTY_SECURITY_ACCESS = "securityAccess";
  private SecurityAccessRequest securityAccess;

  public static final String JSON_PROPERTY_AUTHENTICATION = "authentication";
  private AuthenticationRequest authentication;

  public static final String JSON_PROPERTY_LOG_STORAGE = "logStorage";
  private AwsLogStorageRequest logStorage;

  public static final String JSON_PROPERTY_VPC_ID = "vpcId";
  private String vpcId;

  public static final String JSON_PROPERTY_SUBNET_IDS = "subnetIds";
  private Set<String> subnetIds = new LinkedHashSet<>();

  public static final String JSON_PROPERTY_NETWORK_CIDR = "networkCidr";
  private String networkCidr;

  public static final String JSON_PROPERTY_CREATE_PRIVATE_SUBNETS = "createPrivateSubnets";
  private Boolean createPrivateSubnets;

  public static final String JSON_PROPERTY_CREATE_SERVICE_ENDPOINTS = "createServiceEndpoints";
  private Boolean createServiceEndpoints;

  /**
   * The scheme for the endpoint gateway. PUBLIC creates an external endpoint that can be accessed over the Internet. Defaults to PRIVATE which restricts the traffic to be internal to the VPC.
   */
  public enum EndpointAccessGatewaySchemeEnum {
    PUBLIC("PUBLIC"),
    
    PRIVATE("PRIVATE");

    private String value;

    EndpointAccessGatewaySchemeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static EndpointAccessGatewaySchemeEnum fromValue(String value) {
      for (EndpointAccessGatewaySchemeEnum b : EndpointAccessGatewaySchemeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SCHEME = "endpointAccessGatewayScheme";
  private EndpointAccessGatewaySchemeEnum endpointAccessGatewayScheme;

  public static final String JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS = "endpointAccessGatewaySubnetIds";
  private List<String> endpointAccessGatewaySubnetIds = new ArrayList<>();

  public static final String JSON_PROPERTY_S3_GUARD_TABLE_NAME = "s3GuardTableName";
  private String s3GuardTableName;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  private String description;

  public static final String JSON_PROPERTY_ENABLE_TUNNEL = "enableTunnel";
  private Boolean enableTunnel = true;

  public static final String JSON_PROPERTY_ENABLE_WORKLOAD_ANALYTICS = "enableWorkloadAnalytics";
  private Boolean enableWorkloadAnalytics;

  public static final String JSON_PROPERTY_WORKLOAD_ANALYTICS = "workloadAnalytics";
  private Boolean workloadAnalytics;

  public static final String JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS = "reportDeploymentLogs";
  private Boolean reportDeploymentLogs = false;

  public static final String JSON_PROPERTY_TUNNEL_TYPE = "tunnelType";
  private TunnelType tunnelType;

  public static final String JSON_PROPERTY_CCM_V2_TLS_TYPE = "ccmV2TlsType";
  private CcmV2TlsType ccmV2TlsType;

  public static final String JSON_PROPERTY_CLOUD_STORAGE_LOGGING = "cloudStorageLogging";
  private Boolean cloudStorageLogging;

  public static final String JSON_PROPERTY_FREE_IPA = "freeIpa";
  private AWSFreeIpaCreationRequest freeIpa;

  public static final String JSON_PROPERTY_SECURITY = "security";
  private SecurityRequest security;

  public static final String JSON_PROPERTY_IMAGE = "image";
  private FreeIpaImageRequest image;

  public static final String JSON_PROPERTY_TAGS = "tags";
  private List<TagRequest> tags = new ArrayList<>();

  public static final String JSON_PROPERTY_PROXY_CONFIG_NAME = "proxyConfigName";
  private String proxyConfigName;

  public static final String JSON_PROPERTY_ENCRYPTION_KEY_ARN = "encryptionKeyArn";
  private String encryptionKeyArn;

  public static final String JSON_PROPERTY_ID_BROKER_MAPPING_SOURCE = "idBrokerMappingSource";
  private String idBrokerMappingSource;

  public static final String JSON_PROPERTY_CUSTOM_DOCKER_REGISTRY = "customDockerRegistry";
  private CustomDockerRegistryRequest customDockerRegistry;

  public CreateAWSGovCloudEnvironmentRequest() {
  }

  public CreateAWSGovCloudEnvironmentRequest environmentName(String environmentName) {
    
    this.environmentName = environmentName;
    return this;
  }

   /**
   * The name of the environment. Must contain only lowercase letters, numbers and hyphens.
   * @return environmentName
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ENVIRONMENT_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getEnvironmentName() {
    return environmentName;
  }


  @JsonProperty(JSON_PROPERTY_ENVIRONMENT_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }


  public CreateAWSGovCloudEnvironmentRequest credentialName(String credentialName) {
    
    this.credentialName = credentialName;
    return this;
  }

   /**
   * Name of the credential to use for the environment.
   * @return credentialName
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_CREDENTIAL_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getCredentialName() {
    return credentialName;
  }


  @JsonProperty(JSON_PROPERTY_CREDENTIAL_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setCredentialName(String credentialName) {
    this.credentialName = credentialName;
  }


  public CreateAWSGovCloudEnvironmentRequest region(String region) {
    
    this.region = region;
    return this;
  }

   /**
   * The region of the environment.
   * @return region
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_REGION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getRegion() {
    return region;
  }


  @JsonProperty(JSON_PROPERTY_REGION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setRegion(String region) {
    this.region = region;
  }


  public CreateAWSGovCloudEnvironmentRequest securityAccess(SecurityAccessRequest securityAccess) {
    
    this.securityAccess = securityAccess;
    return this;
  }

   /**
   * Get securityAccess
   * @return securityAccess
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_SECURITY_ACCESS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public SecurityAccessRequest getSecurityAccess() {
    return securityAccess;
  }


  @JsonProperty(JSON_PROPERTY_SECURITY_ACCESS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setSecurityAccess(SecurityAccessRequest securityAccess) {
    this.securityAccess = securityAccess;
  }


  public CreateAWSGovCloudEnvironmentRequest authentication(AuthenticationRequest authentication) {
    
    this.authentication = authentication;
    return this;
  }

   /**
   * Get authentication
   * @return authentication
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_AUTHENTICATION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public AuthenticationRequest getAuthentication() {
    return authentication;
  }


  @JsonProperty(JSON_PROPERTY_AUTHENTICATION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAuthentication(AuthenticationRequest authentication) {
    this.authentication = authentication;
  }


  public CreateAWSGovCloudEnvironmentRequest logStorage(AwsLogStorageRequest logStorage) {
    
    this.logStorage = logStorage;
    return this;
  }

   /**
   * Get logStorage
   * @return logStorage
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_LOG_STORAGE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public AwsLogStorageRequest getLogStorage() {
    return logStorage;
  }


  @JsonProperty(JSON_PROPERTY_LOG_STORAGE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setLogStorage(AwsLogStorageRequest logStorage) {
    this.logStorage = logStorage;
  }


  public CreateAWSGovCloudEnvironmentRequest vpcId(String vpcId) {
    
    this.vpcId = vpcId;
    return this;
  }

   /**
   * The Amazon VPC ID.
   * @return vpcId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_VPC_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getVpcId() {
    return vpcId;
  }


  @JsonProperty(JSON_PROPERTY_VPC_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setVpcId(String vpcId) {
    this.vpcId = vpcId;
  }


  public CreateAWSGovCloudEnvironmentRequest subnetIds(Set<String> subnetIds) {
    
    this.subnetIds = subnetIds;
    return this;
  }

  public CreateAWSGovCloudEnvironmentRequest addSubnetIdsItem(String subnetIdsItem) {
    if (this.subnetIds == null) {
      this.subnetIds = new LinkedHashSet<>();
    }
    this.subnetIds.add(subnetIdsItem);
    return this;
  }

   /**
   * One or more subnet IDs within the VPC.
   * @return subnetIds
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SUBNET_IDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Set<String> getSubnetIds() {
    return subnetIds;
  }


  @JsonDeserialize(as = LinkedHashSet.class)
  @JsonProperty(JSON_PROPERTY_SUBNET_IDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSubnetIds(Set<String> subnetIds) {
    this.subnetIds = subnetIds;
  }


  public CreateAWSGovCloudEnvironmentRequest networkCidr(String networkCidr) {
    
    this.networkCidr = networkCidr;
    return this;
  }

   /**
   * [Deprecated] The network CIDR. This will create a VPC along with subnets in multiple Availability Zones.
   * @return networkCidr
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NETWORK_CIDR)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getNetworkCidr() {
    return networkCidr;
  }


  @JsonProperty(JSON_PROPERTY_NETWORK_CIDR)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNetworkCidr(String networkCidr) {
    this.networkCidr = networkCidr;
  }


  public CreateAWSGovCloudEnvironmentRequest createPrivateSubnets(Boolean createPrivateSubnets) {
    
    this.createPrivateSubnets = createPrivateSubnets;
    return this;
  }

   /**
   * Whether to create private subnets or not.
   * @return createPrivateSubnets
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CREATE_PRIVATE_SUBNETS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getCreatePrivateSubnets() {
    return createPrivateSubnets;
  }


  @JsonProperty(JSON_PROPERTY_CREATE_PRIVATE_SUBNETS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCreatePrivateSubnets(Boolean createPrivateSubnets) {
    this.createPrivateSubnets = createPrivateSubnets;
  }


  public CreateAWSGovCloudEnvironmentRequest createServiceEndpoints(Boolean createServiceEndpoints) {
    
    this.createServiceEndpoints = createServiceEndpoints;
    return this;
  }

   /**
   * Whether to create service endpoints or not.
   * @return createServiceEndpoints
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CREATE_SERVICE_ENDPOINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getCreateServiceEndpoints() {
    return createServiceEndpoints;
  }


  @JsonProperty(JSON_PROPERTY_CREATE_SERVICE_ENDPOINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCreateServiceEndpoints(Boolean createServiceEndpoints) {
    this.createServiceEndpoints = createServiceEndpoints;
  }


  public CreateAWSGovCloudEnvironmentRequest endpointAccessGatewayScheme(EndpointAccessGatewaySchemeEnum endpointAccessGatewayScheme) {
    
    this.endpointAccessGatewayScheme = endpointAccessGatewayScheme;
    return this;
  }

   /**
   * The scheme for the endpoint gateway. PUBLIC creates an external endpoint that can be accessed over the Internet. Defaults to PRIVATE which restricts the traffic to be internal to the VPC.
   * @return endpointAccessGatewayScheme
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SCHEME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public EndpointAccessGatewaySchemeEnum getEndpointAccessGatewayScheme() {
    return endpointAccessGatewayScheme;
  }


  @JsonProperty(JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SCHEME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndpointAccessGatewayScheme(EndpointAccessGatewaySchemeEnum endpointAccessGatewayScheme) {
    this.endpointAccessGatewayScheme = endpointAccessGatewayScheme;
  }


  public CreateAWSGovCloudEnvironmentRequest endpointAccessGatewaySubnetIds(List<String> endpointAccessGatewaySubnetIds) {
    
    this.endpointAccessGatewaySubnetIds = endpointAccessGatewaySubnetIds;
    return this;
  }

  public CreateAWSGovCloudEnvironmentRequest addEndpointAccessGatewaySubnetIdsItem(String endpointAccessGatewaySubnetIdsItem) {
    if (this.endpointAccessGatewaySubnetIds == null) {
      this.endpointAccessGatewaySubnetIds = new ArrayList<>();
    }
    this.endpointAccessGatewaySubnetIds.add(endpointAccessGatewaySubnetIdsItem);
    return this;
  }

   /**
   * The subnets to use for endpoint access gateway.
   * @return endpointAccessGatewaySubnetIds
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getEndpointAccessGatewaySubnetIds() {
    return endpointAccessGatewaySubnetIds;
  }


  @JsonProperty(JSON_PROPERTY_ENDPOINT_ACCESS_GATEWAY_SUBNET_IDS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndpointAccessGatewaySubnetIds(List<String> endpointAccessGatewaySubnetIds) {
    this.endpointAccessGatewaySubnetIds = endpointAccessGatewaySubnetIds;
  }


  public CreateAWSGovCloudEnvironmentRequest s3GuardTableName(String s3GuardTableName) {
    
    this.s3GuardTableName = s3GuardTableName;
    return this;
  }

   /**
   * Deprecated. S3Guard was used to ensure consistent S3 updates when S3 was still eventually consistent. With the introduction of Consistent S3, the goal and usage of S3 Guard have become superfluous and defunct.
   * @return s3GuardTableName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_S3_GUARD_TABLE_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getS3GuardTableName() {
    return s3GuardTableName;
  }


  @JsonProperty(JSON_PROPERTY_S3_GUARD_TABLE_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setS3GuardTableName(String s3GuardTableName) {
    this.s3GuardTableName = s3GuardTableName;
  }


  public CreateAWSGovCloudEnvironmentRequest description(String description) {
    
    this.description = description;
    return this;
  }

   /**
   * An description of the environment.
   * @return description
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDescription() {
    return description;
  }


  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDescription(String description) {
    this.description = description;
  }


  public CreateAWSGovCloudEnvironmentRequest enableTunnel(Boolean enableTunnel) {
    
    this.enableTunnel = enableTunnel;
    return this;
  }

   /**
   * Whether to enable SSH tunneling for the environment.
   * @return enableTunnel
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENABLE_TUNNEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getEnableTunnel() {
    return enableTunnel;
  }


  @JsonProperty(JSON_PROPERTY_ENABLE_TUNNEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnableTunnel(Boolean enableTunnel) {
    this.enableTunnel = enableTunnel;
  }


  public CreateAWSGovCloudEnvironmentRequest enableWorkloadAnalytics(Boolean enableWorkloadAnalytics) {
    
    this.enableWorkloadAnalytics = enableWorkloadAnalytics;
    return this;
  }

   /**
   * When this is enabled, diagnostic information about job and query execution is sent to Workload Manager for Data Hub clusters created within this environment.
   * @return enableWorkloadAnalytics
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENABLE_WORKLOAD_ANALYTICS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getEnableWorkloadAnalytics() {
    return enableWorkloadAnalytics;
  }


  @JsonProperty(JSON_PROPERTY_ENABLE_WORKLOAD_ANALYTICS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnableWorkloadAnalytics(Boolean enableWorkloadAnalytics) {
    this.enableWorkloadAnalytics = enableWorkloadAnalytics;
  }


  public CreateAWSGovCloudEnvironmentRequest workloadAnalytics(Boolean workloadAnalytics) {
    
    this.workloadAnalytics = workloadAnalytics;
    return this;
  }

   /**
   * When this is enabled, diagnostic information about job and query execution is sent to Workload Manager for Data Hub clusters created within this environment.
   * @return workloadAnalytics
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WORKLOAD_ANALYTICS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getWorkloadAnalytics() {
    return workloadAnalytics;
  }


  @JsonProperty(JSON_PROPERTY_WORKLOAD_ANALYTICS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWorkloadAnalytics(Boolean workloadAnalytics) {
    this.workloadAnalytics = workloadAnalytics;
  }


  public CreateAWSGovCloudEnvironmentRequest reportDeploymentLogs(Boolean reportDeploymentLogs) {
    
    this.reportDeploymentLogs = reportDeploymentLogs;
    return this;
  }

   /**
   * [Deprecated] When true, this will report additional diagnostic information back to Cloudera.
   * @return reportDeploymentLogs
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getReportDeploymentLogs() {
    return reportDeploymentLogs;
  }


  @JsonProperty(JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setReportDeploymentLogs(Boolean reportDeploymentLogs) {
    this.reportDeploymentLogs = reportDeploymentLogs;
  }


  public CreateAWSGovCloudEnvironmentRequest tunnelType(TunnelType tunnelType) {
    
    this.tunnelType = tunnelType;
    return this;
  }

   /**
   * Get tunnelType
   * @return tunnelType
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TUNNEL_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public TunnelType getTunnelType() {
    return tunnelType;
  }


  @JsonProperty(JSON_PROPERTY_TUNNEL_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTunnelType(TunnelType tunnelType) {
    this.tunnelType = tunnelType;
  }


  public CreateAWSGovCloudEnvironmentRequest ccmV2TlsType(CcmV2TlsType ccmV2TlsType) {
    
    this.ccmV2TlsType = ccmV2TlsType;
    return this;
  }

   /**
   * Get ccmV2TlsType
   * @return ccmV2TlsType
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CCM_V2_TLS_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public CcmV2TlsType getCcmV2TlsType() {
    return ccmV2TlsType;
  }


  @JsonProperty(JSON_PROPERTY_CCM_V2_TLS_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
    this.ccmV2TlsType = ccmV2TlsType;
  }


  public CreateAWSGovCloudEnvironmentRequest cloudStorageLogging(Boolean cloudStorageLogging) {
    
    this.cloudStorageLogging = cloudStorageLogging;
    return this;
  }

   /**
   * When this is enabled, logs from the VMs will end up on the pre-defined cloud storage (enabled by default).
   * @return cloudStorageLogging
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CLOUD_STORAGE_LOGGING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getCloudStorageLogging() {
    return cloudStorageLogging;
  }


  @JsonProperty(JSON_PROPERTY_CLOUD_STORAGE_LOGGING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCloudStorageLogging(Boolean cloudStorageLogging) {
    this.cloudStorageLogging = cloudStorageLogging;
  }


  public CreateAWSGovCloudEnvironmentRequest freeIpa(AWSFreeIpaCreationRequest freeIpa) {
    
    this.freeIpa = freeIpa;
    return this;
  }

   /**
   * Get freeIpa
   * @return freeIpa
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FREE_IPA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public AWSFreeIpaCreationRequest getFreeIpa() {
    return freeIpa;
  }


  @JsonProperty(JSON_PROPERTY_FREE_IPA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFreeIpa(AWSFreeIpaCreationRequest freeIpa) {
    this.freeIpa = freeIpa;
  }


  public CreateAWSGovCloudEnvironmentRequest security(SecurityRequest security) {
    
    this.security = security;
    return this;
  }

   /**
   * Get security
   * @return security
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SECURITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public SecurityRequest getSecurity() {
    return security;
  }


  @JsonProperty(JSON_PROPERTY_SECURITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSecurity(SecurityRequest security) {
    this.security = security;
  }


  public CreateAWSGovCloudEnvironmentRequest image(FreeIpaImageRequest image) {
    
    this.image = image;
    return this;
  }

   /**
   * Get image
   * @return image
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_IMAGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public FreeIpaImageRequest getImage() {
    return image;
  }


  @JsonProperty(JSON_PROPERTY_IMAGE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setImage(FreeIpaImageRequest image) {
    this.image = image;
  }


  public CreateAWSGovCloudEnvironmentRequest tags(List<TagRequest> tags) {
    
    this.tags = tags;
    return this;
  }

  public CreateAWSGovCloudEnvironmentRequest addTagsItem(TagRequest tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

   /**
   * Tags associated with the resources.
   * @return tags
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TAGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<TagRequest> getTags() {
    return tags;
  }


  @JsonProperty(JSON_PROPERTY_TAGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTags(List<TagRequest> tags) {
    this.tags = tags;
  }


  public CreateAWSGovCloudEnvironmentRequest proxyConfigName(String proxyConfigName) {
    
    this.proxyConfigName = proxyConfigName;
    return this;
  }

   /**
   * Name of the proxy config to use for the environment.
   * @return proxyConfigName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROXY_CONFIG_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getProxyConfigName() {
    return proxyConfigName;
  }


  @JsonProperty(JSON_PROPERTY_PROXY_CONFIG_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProxyConfigName(String proxyConfigName) {
    this.proxyConfigName = proxyConfigName;
  }


  public CreateAWSGovCloudEnvironmentRequest encryptionKeyArn(String encryptionKeyArn) {
    
    this.encryptionKeyArn = encryptionKeyArn;
    return this;
  }

   /**
   * ARN of the AWS KMS CMK to use for the server-side encryption of AWS storage resources.
   * @return encryptionKeyArn
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_ARN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEncryptionKeyArn() {
    return encryptionKeyArn;
  }


  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_ARN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEncryptionKeyArn(String encryptionKeyArn) {
    this.encryptionKeyArn = encryptionKeyArn;
  }


  public CreateAWSGovCloudEnvironmentRequest idBrokerMappingSource(String idBrokerMappingSource) {
    
    this.idBrokerMappingSource = idBrokerMappingSource;
    return this;
  }

   /**
   * This is an optional field. This is for QE testing purposes and internal use only.
   * @return idBrokerMappingSource
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ID_BROKER_MAPPING_SOURCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getIdBrokerMappingSource() {
    return idBrokerMappingSource;
  }


  @JsonProperty(JSON_PROPERTY_ID_BROKER_MAPPING_SOURCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIdBrokerMappingSource(String idBrokerMappingSource) {
    this.idBrokerMappingSource = idBrokerMappingSource;
  }


  public CreateAWSGovCloudEnvironmentRequest customDockerRegistry(CustomDockerRegistryRequest customDockerRegistry) {
    
    this.customDockerRegistry = customDockerRegistry;
    return this;
  }

   /**
   * Get customDockerRegistry
   * @return customDockerRegistry
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CUSTOM_DOCKER_REGISTRY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public CustomDockerRegistryRequest getCustomDockerRegistry() {
    return customDockerRegistry;
  }


  @JsonProperty(JSON_PROPERTY_CUSTOM_DOCKER_REGISTRY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCustomDockerRegistry(CustomDockerRegistryRequest customDockerRegistry) {
    this.customDockerRegistry = customDockerRegistry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateAWSGovCloudEnvironmentRequest createAWSGovCloudEnvironmentRequest = (CreateAWSGovCloudEnvironmentRequest) o;
    return Objects.equals(this.environmentName, createAWSGovCloudEnvironmentRequest.environmentName) &&
        Objects.equals(this.credentialName, createAWSGovCloudEnvironmentRequest.credentialName) &&
        Objects.equals(this.region, createAWSGovCloudEnvironmentRequest.region) &&
        Objects.equals(this.securityAccess, createAWSGovCloudEnvironmentRequest.securityAccess) &&
        Objects.equals(this.authentication, createAWSGovCloudEnvironmentRequest.authentication) &&
        Objects.equals(this.logStorage, createAWSGovCloudEnvironmentRequest.logStorage) &&
        Objects.equals(this.vpcId, createAWSGovCloudEnvironmentRequest.vpcId) &&
        Objects.equals(this.subnetIds, createAWSGovCloudEnvironmentRequest.subnetIds) &&
        Objects.equals(this.networkCidr, createAWSGovCloudEnvironmentRequest.networkCidr) &&
        Objects.equals(this.createPrivateSubnets, createAWSGovCloudEnvironmentRequest.createPrivateSubnets) &&
        Objects.equals(this.createServiceEndpoints, createAWSGovCloudEnvironmentRequest.createServiceEndpoints) &&
        Objects.equals(this.endpointAccessGatewayScheme, createAWSGovCloudEnvironmentRequest.endpointAccessGatewayScheme) &&
        Objects.equals(this.endpointAccessGatewaySubnetIds, createAWSGovCloudEnvironmentRequest.endpointAccessGatewaySubnetIds) &&
        Objects.equals(this.s3GuardTableName, createAWSGovCloudEnvironmentRequest.s3GuardTableName) &&
        Objects.equals(this.description, createAWSGovCloudEnvironmentRequest.description) &&
        Objects.equals(this.enableTunnel, createAWSGovCloudEnvironmentRequest.enableTunnel) &&
        Objects.equals(this.enableWorkloadAnalytics, createAWSGovCloudEnvironmentRequest.enableWorkloadAnalytics) &&
        Objects.equals(this.workloadAnalytics, createAWSGovCloudEnvironmentRequest.workloadAnalytics) &&
        Objects.equals(this.reportDeploymentLogs, createAWSGovCloudEnvironmentRequest.reportDeploymentLogs) &&
        Objects.equals(this.tunnelType, createAWSGovCloudEnvironmentRequest.tunnelType) &&
        Objects.equals(this.ccmV2TlsType, createAWSGovCloudEnvironmentRequest.ccmV2TlsType) &&
        Objects.equals(this.cloudStorageLogging, createAWSGovCloudEnvironmentRequest.cloudStorageLogging) &&
        Objects.equals(this.freeIpa, createAWSGovCloudEnvironmentRequest.freeIpa) &&
        Objects.equals(this.security, createAWSGovCloudEnvironmentRequest.security) &&
        Objects.equals(this.image, createAWSGovCloudEnvironmentRequest.image) &&
        Objects.equals(this.tags, createAWSGovCloudEnvironmentRequest.tags) &&
        Objects.equals(this.proxyConfigName, createAWSGovCloudEnvironmentRequest.proxyConfigName) &&
        Objects.equals(this.encryptionKeyArn, createAWSGovCloudEnvironmentRequest.encryptionKeyArn) &&
        Objects.equals(this.idBrokerMappingSource, createAWSGovCloudEnvironmentRequest.idBrokerMappingSource) &&
        Objects.equals(this.customDockerRegistry, createAWSGovCloudEnvironmentRequest.customDockerRegistry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environmentName, credentialName, region, securityAccess, authentication, logStorage, vpcId, subnetIds, networkCidr, createPrivateSubnets, createServiceEndpoints, endpointAccessGatewayScheme, endpointAccessGatewaySubnetIds, s3GuardTableName, description, enableTunnel, enableWorkloadAnalytics, workloadAnalytics, reportDeploymentLogs, tunnelType, ccmV2TlsType, cloudStorageLogging, freeIpa, security, image, tags, proxyConfigName, encryptionKeyArn, idBrokerMappingSource, customDockerRegistry);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateAWSGovCloudEnvironmentRequest {\n");
    sb.append("    environmentName: ").append(toIndentedString(environmentName)).append("\n");
    sb.append("    credentialName: ").append(toIndentedString(credentialName)).append("\n");
    sb.append("    region: ").append(toIndentedString(region)).append("\n");
    sb.append("    securityAccess: ").append(toIndentedString(securityAccess)).append("\n");
    sb.append("    authentication: ").append(toIndentedString(authentication)).append("\n");
    sb.append("    logStorage: ").append(toIndentedString(logStorage)).append("\n");
    sb.append("    vpcId: ").append(toIndentedString(vpcId)).append("\n");
    sb.append("    subnetIds: ").append(toIndentedString(subnetIds)).append("\n");
    sb.append("    networkCidr: ").append(toIndentedString(networkCidr)).append("\n");
    sb.append("    createPrivateSubnets: ").append(toIndentedString(createPrivateSubnets)).append("\n");
    sb.append("    createServiceEndpoints: ").append(toIndentedString(createServiceEndpoints)).append("\n");
    sb.append("    endpointAccessGatewayScheme: ").append(toIndentedString(endpointAccessGatewayScheme)).append("\n");
    sb.append("    endpointAccessGatewaySubnetIds: ").append(toIndentedString(endpointAccessGatewaySubnetIds)).append("\n");
    sb.append("    s3GuardTableName: ").append(toIndentedString(s3GuardTableName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    enableTunnel: ").append(toIndentedString(enableTunnel)).append("\n");
    sb.append("    enableWorkloadAnalytics: ").append(toIndentedString(enableWorkloadAnalytics)).append("\n");
    sb.append("    workloadAnalytics: ").append(toIndentedString(workloadAnalytics)).append("\n");
    sb.append("    reportDeploymentLogs: ").append(toIndentedString(reportDeploymentLogs)).append("\n");
    sb.append("    tunnelType: ").append(toIndentedString(tunnelType)).append("\n");
    sb.append("    ccmV2TlsType: ").append(toIndentedString(ccmV2TlsType)).append("\n");
    sb.append("    cloudStorageLogging: ").append(toIndentedString(cloudStorageLogging)).append("\n");
    sb.append("    freeIpa: ").append(toIndentedString(freeIpa)).append("\n");
    sb.append("    security: ").append(toIndentedString(security)).append("\n");
    sb.append("    image: ").append(toIndentedString(image)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    proxyConfigName: ").append(toIndentedString(proxyConfigName)).append("\n");
    sb.append("    encryptionKeyArn: ").append(toIndentedString(encryptionKeyArn)).append("\n");
    sb.append("    idBrokerMappingSource: ").append(toIndentedString(idBrokerMappingSource)).append("\n");
    sb.append("    customDockerRegistry: ").append(toIndentedString(customDockerRegistry)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

