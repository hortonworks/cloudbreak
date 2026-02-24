package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.DatalakeCrn;
import com.sequenceiq.cloudbreak.validation.ValidEnvironmentCrn;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.Architecture;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackV4Request extends StackV4Base implements TaggableRequest {

    @ValidEnvironmentCrn
    @Schema(description = StackModelDescription.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @Schema(description = StackModelDescription.CUSTOM_DOMAIN_SETTINGS)
    private CustomDomainSettingsV4Request customDomain;

    @Schema(description = StackModelDescription.TAGS)
    private TagsV4Request tags;

    @Schema(description = StackModelDescription.TELEMETRY)
    private TelemetryRequest telemetry;

    @Valid
    @Schema(description = PLACEMENT_SETTINGS)
    private PlacementSettingsV4Request placement;

    @NotNull
    @Valid
    @Schema(description = StackModelDescription.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();

    @NotNull(message = "You should define authentication for stack!")
    @Valid
    @Schema(description = StackModelDescription.AUTHENTICATION)
    private StackAuthenticationV4Request authentication;

    @Valid
    @Schema(description = StackModelDescription.NETWORK)
    private NetworkV4Request network;

    @Schema(description = StackModelDescription.IMAGE_SETTINGS)
    private ImageSettingsV4Request image;

    @Valid
    @Schema(description = StackModelDescription.CLUSTER_REQUEST)
    private ClusterV4Request cluster;

    @Schema(description = StackModelDescription.GATEWAY_PORT)
    @Min(value = 1025, message = "Port should be between 1025 and 65535")
    @Max(value = 65535, message = "Port should be between 1025 and 65535")
    private Integer gatewayPort;

    private StackType type = StackType.WORKLOAD;

    @Schema(description = ClusterModelDescription.SHARED_SERVICE_REQUEST)
    private SharedServiceV4Request sharedService;

    @Schema(description = StackModelDescription.INPUTS)
    private Map<String, Object> inputs = new HashMap<>();

    @Valid
    @Schema(description = StackModelDescription.EXTERNAL_DATABASE)
    private DatabaseRequest externalDatabase;

    @Schema(description = StackModelDescription.ENABLE_LOAD_BALANCER)
    private boolean enableLoadBalancer;

    @DatalakeCrn
    @Schema(description = StackModelDescription.RESOURCE_CRN)
    private String resourceCrn;

    private String variant;

    @Schema(description = StackModelDescription.JAVA_VERSION)
    private Integer javaVersion;

    @Schema(description = StackModelDescription.MULTIPLE_AVAILABILITY_ZONES)
    private boolean enableMultiAz;

    @Schema(description = ModelDescriptions.ARCHITECTURE)
    private String architecture;

    @Schema(description = ModelDescriptions.NOTIFICATION_STATE)
    private NotificationState notificationState;

    @Schema(description = ModelDescriptions.Database.DISABLE_DB_SSL_ENFORCEMENT)
    private boolean disableDbSslEnforcement;

    @Schema(description = StackModelDescription.SECURITY)
    private SecurityV4Request security;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public CustomDomainSettingsV4Request getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(CustomDomainSettingsV4Request customDomain) {
        this.customDomain = customDomain;
    }

    public TagsV4Request getTags() {
        return tags;
    }

    public TagsV4Request initAndGetTags() {
        if (tags == null) {
            tags = new TagsV4Request();
        }
        return tags;
    }

    public void setTags(TagsV4Request tags) {
        this.tags = tags;
    }

    @Override
    public void addTag(String key, String value) {
        initAndGetTags().getUserDefined().put(key, value);
    }

    public List<InstanceGroupV4Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupV4Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public StackAuthenticationV4Request getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationV4Request authentication) {
        this.authentication = authentication;
    }

    public NetworkV4Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4Request network) {
        this.network = network;
    }

    public ImageSettingsV4Request getImage() {
        return image;
    }

    public void setImage(ImageSettingsV4Request image) {
        this.image = image;
    }

    public ClusterV4Request getCluster() {
        return cluster;
    }

    public void setCluster(ClusterV4Request cluster) {
        this.cluster = cluster;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    public PlacementSettingsV4Request getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettingsV4Request placement) {
        this.placement = placement;
    }

    public SharedServiceV4Request getSharedService() {
        return sharedService;
    }

    public void setSharedService(SharedServiceV4Request sharedService) {
        this.sharedService = sharedService;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public TelemetryRequest getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryRequest telemetry) {
        this.telemetry = telemetry;
    }

    public DatabaseRequest getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(DatabaseRequest externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    public boolean isEnableLoadBalancer() {
        return enableLoadBalancer;
    }

    public void setEnableLoadBalancer(boolean enableLoadBalancer) {
        this.enableLoadBalancer = enableLoadBalancer;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Integer getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(Integer javaVersion) {
        this.javaVersion = javaVersion;
    }

    public boolean isEnableMultiAz() {
        return enableMultiAz;
    }

    public void setEnableMultiAz(boolean enableMultiAz) {
        this.enableMultiAz = enableMultiAz;
    }

    public String getArchitecture() {
        return architecture;
    }

    @Schema(hidden = true)
    public Architecture getArchitectureEnum() {
        return Architecture.fromStringWithValidation(architecture);
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public boolean isDisableDbSslEnforcement() {
        return disableDbSslEnforcement;
    }

    public void setDisableDbSslEnforcement(boolean disableDbSslEnforcement) {
        this.disableDbSslEnforcement = disableDbSslEnforcement;
    }

    public SecurityV4Request getSecurity() {
        return security;
    }

    public void setSecurity(SecurityV4Request security) {
        this.security = security;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationState = notificationState;
    }
}
