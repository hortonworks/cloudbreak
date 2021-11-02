package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("DescribeFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeFreeIpaResponse {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_NAME, required = true)
    private String name;

    @NotNull
    private String crn;

    @NotNull
    @ApiModelProperty(FreeIpaModelDescriptions.PLACEMENT_SETTINGS)
    private PlacementResponse placement;

    @ApiModelProperty(value = FreeIpaModelDescriptions.TUNNEL)
    private Tunnel tunnel;

    @NotNull
    @Valid
    @ApiModelProperty(value = FreeIpaModelDescriptions.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupResponse> instanceGroups;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.AUTHENTICATION, required = true)
    private StackAuthenticationResponse authentication;

    @Valid
    @ApiModelProperty(FreeIpaModelDescriptions.NETWORK)
    private NetworkResponse network;

    @ApiModelProperty(FreeIpaModelDescriptions.IMAGE_SETTINGS)
    private ImageSettingsResponse image;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_SERVER_SETTINGS, required = true)
    private FreeIpaServerResponse freeIpa;

    private AvailabilityStatus availabilityStatus;

    private Status status;

    private String statusString;

    private String statusReason;

    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_APPLICATION_VERSION)
    private String appVersion;

    @ApiModelProperty(value = FreeIpaModelDescriptions.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(value = FreeIpaModelDescriptions.VARIANT)
    private String variant;

    @ApiModelProperty(value = FreeIpaModelDescriptions.TELEMETRY)
    private TelemetryResponse telemetry;

    @ApiModelProperty(value = FreeIpaModelDescriptions.CLOUD_STORAGE)
    private CloudStorageResponse cloudStorage;

    @ApiModelProperty(value = FreeIpaModelDescriptions.USERSYNC_STATUS_DETAILS)
    private UserSyncStatusResponse userSyncStatus;

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

    @Override
    public String toString() {
        return "DescribeFreeIpaResponse{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", placement=" + placement +
                ", instanceGroups=" + instanceGroups +
                ", authentication=" + authentication +
                ", network=" + network +
                ", image=" + image +
                ", freeIpa=" + freeIpa +
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
                '}';
    }
}
