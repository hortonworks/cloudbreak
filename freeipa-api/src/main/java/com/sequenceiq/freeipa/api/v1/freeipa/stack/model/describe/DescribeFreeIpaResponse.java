package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.usersync.UserSyncStatusResponse;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DescribeFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeFreeIpaResponse {
    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.FREEIPA_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.PLACEMENT_SETTINGS, requiredMode = Schema.RequiredMode.REQUIRED)
    private PlacementResponse placement;

    @Schema(description = FreeIpaModelDescriptions.TUNNEL)
    private Tunnel tunnel;

    @NotNull
    @Valid
    @Schema(description = FreeIpaModelDescriptions.INSTANCE_GROUPS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<InstanceGroupResponse> instanceGroups = new ArrayList<>();

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.AUTHENTICATION, requiredMode = Schema.RequiredMode.REQUIRED)
    private StackAuthenticationResponse authentication;

    @Valid
    @Schema(description = FreeIpaModelDescriptions.NETWORK)
    private NetworkResponse network;

    @Schema(description = FreeIpaModelDescriptions.IMAGE_SETTINGS)
    private ImageSettingsResponse image;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.FREEIPA_SERVER_SETTINGS, requiredMode = Schema.RequiredMode.REQUIRED)
    private FreeIpaServerResponse freeIpa;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> recipes = new HashSet<>();

    private AvailabilityStatus availabilityStatus;

    private Status status;

    private String statusString;

    private String statusReason;

    @Schema(description = FreeIpaModelDescriptions.FREEIPA_APPLICATION_VERSION)
    private String appVersion;

    @Schema(description = FreeIpaModelDescriptions.CLOUD_PLATFORM)
    private String cloudPlatform;

    @Schema(description = FreeIpaModelDescriptions.VARIANT)
    private String variant;

    @Schema(description = FreeIpaModelDescriptions.TELEMETRY)
    private TelemetryResponse telemetry;

    @Schema(description = FreeIpaModelDescriptions.CLOUD_STORAGE)
    private CloudStorageResponse cloudStorage;

    @Schema(description = FreeIpaModelDescriptions.USERSYNC_STATUS_DETAILS)
    private UserSyncStatusResponse userSyncStatus;

    @Schema(description = FreeIpaModelDescriptions.MULTIAZ, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableMultiAz;

    @Schema(description = FreeIpaModelDescriptions.SUPPORTED_IMDS_VERSION)
    private String supportedImdsVersion;

    @Schema(description = FreeIpaModelDescriptions.FreeIpaImageSecurityModelDescriptions.IMAGE_SECURITY)
    private SecurityResponse security;

    @Schema(description = FreeIpaModelDescriptions.LOADBALANCER_DETAILS)
    private FreeIpaLoadBalancerResponse loadBalancer;

    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.TRUST_DETAILS, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private TrustResponse trust;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlacementResponse getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementResponse placement) {
        this.placement = placement;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public List<InstanceGroupResponse> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupResponse> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public StackAuthenticationResponse getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationResponse authentication) {
        this.authentication = authentication;
    }

    public NetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(NetworkResponse network) {
        this.network = network;
    }

    public ImageSettingsResponse getImage() {
        return image;
    }

    public void setImage(ImageSettingsResponse image) {
        this.image = image;
    }

    public FreeIpaServerResponse getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpaServerResponse freeIpa) {
        this.freeIpa = freeIpa;
    }

    public TelemetryResponse getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryResponse telemetry) {
        this.telemetry = telemetry;
    }

    public CloudStorageResponse getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageResponse cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getStatusString() {
        return statusString;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public UserSyncStatusResponse getUserSyncStatus() {
        return userSyncStatus;
    }

    public void setUserSyncStatus(UserSyncStatusResponse userSyncStatus) {
        this.userSyncStatus = userSyncStatus;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Set<String> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<String> recipes) {
        this.recipes = recipes;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public String getSupportedImdsVersion() {
        return supportedImdsVersion;
    }

    public void setSupportedImdsVersion(String supportedImdsVersion) {
        this.supportedImdsVersion = supportedImdsVersion;
    }

    public SecurityResponse getSecurity() {
        return security;
    }

    public void setSecurity(SecurityResponse security) {
        this.security = security;
    }

    public FreeIpaLoadBalancerResponse getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(FreeIpaLoadBalancerResponse loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public TrustResponse getTrust() {
        return trust;
    }

    public void setTrust(TrustResponse trust) {
        this.trust = trust;
    }

    @Override
    public String toString() {
        return "DescribeFreeIpaResponse{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", placement=" + placement +
                ", tunnel=" + tunnel +
                ", instanceGroups=" + instanceGroups +
                ", authentication=" + authentication +
                ", network=" + network +
                ", image=" + image +
                ", freeIpa=" + freeIpa +
                ", recipes=" + recipes +
                ", availabilityStatus=" + availabilityStatus +
                ", status=" + status +
                ", statusString='" + statusString + '\'' +
                ", statusReason='" + statusReason + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", variant='" + variant + '\'' +
                ", telemetry=" + telemetry +
                ", cloudStorage=" + cloudStorage +
                ", userSyncStatus=" + userSyncStatus +
                ", enableMultiAz=" + enableMultiAz +
                ", supportedImdsVersion='" + supportedImdsVersion + '\'' +
                ", security=" + security +
                ", loadBalancer=" + loadBalancer +
                ", trust=" + trust +
                '}';
    }
}
