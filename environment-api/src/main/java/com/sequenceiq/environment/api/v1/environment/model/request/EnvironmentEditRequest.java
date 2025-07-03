package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentEditV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentEditRequest implements Serializable {

    @Size(max = 1000)
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkRequest network;

    @Schema(description = EnvironmentModelDescription.AUTHENTICATION)
    private @Valid EnvironmentAuthenticationRequest authentication;

    @Schema(description = EnvironmentModelDescription.TELEMETRY)
    private TelemetryRequest telemetry;

    @Schema(description = EnvironmentModelDescription.BACKUP)
    private @Valid BackupRequest backup;

    @Schema(description = EnvironmentModelDescription.SECURITY_ACCESS)
    private @Valid SecurityAccessRequest securityAccess;

    @Schema(description = EnvironmentModelDescription.IDBROKER_MAPPING_SOURCE)
    private IdBrokerMappingSource idBrokerMappingSource;

    @Schema(description = EnvironmentModelDescription.CLOUD_STORAGE_VALIDATION)
    private CloudStorageValidation cloudStorageValidation;

    @Schema(description = EnvironmentModelDescription.ADMIN_GROUP_NAME)
    private String adminGroupName;

    private ProxyRequest proxy;

    @Valid
    @Schema(description = EnvironmentModelDescription.AWS_PARAMETERS)
    private AwsEnvironmentParameters aws;

    @Valid
    @Schema(description = EnvironmentModelDescription.AZURE_PARAMETERS)
    private AzureEnvironmentParameters azure;

    @Valid
    @Schema(description = EnvironmentModelDescription.GCP_PARAMETERS)
    private GcpEnvironmentParameters gcp;

    @Valid
    @Schema(description = EnvironmentModelDescription.TAGS)
    private Map<String, String> tags = new HashMap<>();

    @Valid
    @Schema(description = EnvironmentModelDescription.DATA_SERVICES)
    private DataServicesRequest dataServices;

    @Schema(description = EnvironmentModelDescription.HYBRID_ENVIRONMENT)
    private HybridEnvironmentRequest hybridEnvironment;

    @Schema(description = EnvironmentModelDescription.FREEIPA_NODE_COUNT)
    private Integer freeIpaNodeCount;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EnvironmentNetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkRequest network) {
        this.network = network;
    }

    public EnvironmentAuthenticationRequest getAuthentication() {
        return authentication;
    }

    public void setAuthentication(EnvironmentAuthenticationRequest authentication) {
        this.authentication = authentication;
    }

    public TelemetryRequest getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryRequest telemetry) {
        this.telemetry = telemetry;
    }

    public BackupRequest getBackup() {
        return backup;
    }

    public void setBackup(BackupRequest backup) {
        this.backup = backup;
    }

    public SecurityAccessRequest getSecurityAccess() {
        return securityAccess;
    }

    public void setSecurityAccess(SecurityAccessRequest securityAccess) {
        this.securityAccess = securityAccess;
    }

    public IdBrokerMappingSource getIdBrokerMappingSource() {
        return idBrokerMappingSource;
    }

    public void setIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
        this.idBrokerMappingSource = idBrokerMappingSource;
    }

    public CloudStorageValidation getCloudStorageValidation() {
        return cloudStorageValidation;
    }

    public void setCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
        this.cloudStorageValidation = cloudStorageValidation;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public ProxyRequest getProxy() {
        return proxy;
    }

    public void setProxy(ProxyRequest proxy) {
        this.proxy = proxy;
    }

    public AwsEnvironmentParameters getAws() {
        return aws;
    }

    public void setAws(AwsEnvironmentParameters aws) {
        this.aws = aws;
    }

    public AzureEnvironmentParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureEnvironmentParameters azure) {
        this.azure = azure;
    }

    public GcpEnvironmentParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpEnvironmentParameters gcp) {
        this.gcp = gcp;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public DataServicesRequest getDataServices() {
        return dataServices;
    }

    public void setDataServices(DataServicesRequest dataServices) {
        this.dataServices = dataServices;
    }

    public Integer getFreeIpaNodeCount() {
        return freeIpaNodeCount;
    }

    public void setFreeIpaNodeCount(Integer freeIpaNodeCount) {
        this.freeIpaNodeCount = freeIpaNodeCount;
    }

    public HybridEnvironmentRequest getHybridEnvironment() {
        return hybridEnvironment;
    }

    public void setHybridEnvironment(HybridEnvironmentRequest hybridEnvironment) {
        this.hybridEnvironment = hybridEnvironment;
    }

    @Override
    public String toString() {
        return "EnvironmentEditRequest{" +
                "description='" + description + '\'' +
                ", network=" + network +
                ", authentication=" + authentication +
                ", telemetry=" + telemetry +
                ", backup=" + backup +
                ", securityAccess=" + securityAccess +
                ", idBrokerMappingSource=" + idBrokerMappingSource +
                ", cloudStorageValidation=" + cloudStorageValidation +
                ", adminGroupName='" + adminGroupName + '\'' +
                ", proxy=" + proxy +
                ", aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                ", tags=" + tags +
                ", dataServices=" + dataServices +
                ", freeipaNodeCount=" + freeIpaNodeCount +
                ", hybridEnvironment=" + hybridEnvironment +
                '}';
    }
}
