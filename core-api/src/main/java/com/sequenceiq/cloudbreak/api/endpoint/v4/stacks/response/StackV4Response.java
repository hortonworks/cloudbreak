package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackV4Response extends StackV4Base implements TaggedResponse {

    private Long id;

    private String crn;

    private String environmentCrn;

    private String environmentName;

    private String environmentType;

    private String credentialName;

    private String credentialCrn;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean govCloud;

    private String region;

    @Schema(description = StackModelDescription.STACK_STATUS)
    private Status status;

    @Schema(description = StackModelDescription.CLUSTER)
    private ClusterV4Response cluster;

    @Schema(description = StackModelDescription.STATUS_REASON)
    private String statusReason;

    @Schema(description = StackModelDescription.NETWORK)
    private NetworkV4Response network;

    @Valid
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();

    @Schema(description = StackModelDescription.CREATED)
    private Long created;

    @Schema(description = StackModelDescription.TERMINATED)
    private Long terminated;

    @Schema(description = StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @Schema(description = StackModelDescription.IMAGE)
    private StackImageV4Response image;

    @Schema(description = StackModelDescription.CLOUDBREAK_DETAILS)
    private CloudbreakDetailsV4Response cloudbreakDetails;

    @Schema(description = StackModelDescription.AUTHENTICATION)
    private StackAuthenticationV4Response authentication;

    @Schema(description = StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @Schema(description = StackModelDescription.HARDWARE_INFO_RESPONSE, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<HardwareInfoGroupV4Response> hardwareInfoGroups = new HashSet<>();

    @Schema(description = StackModelDescription.EVENTS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CloudbreakEventV4Response> cloudbreakEvents = new ArrayList<>();

    @Schema(description = StackModelDescription.TAGS)
    private TagsV4Response tags;

    @Schema(description = StackModelDescription.TELEMETRY)
    private TelemetryResponse telemetry;

    @Schema(description = ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @Schema
    private CustomDomainSettingsV4Response customDomains;

    @NotNull
    @Schema(description = PLACEMENT_SETTINGS, requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private PlacementSettingsV4Response placement;

    @Deprecated
    @Schema(description = ClusterModelDescription.SHARED_SERVICE_REQUEST)
    private SharedServiceV4Response sharedService;

    @Schema(description = StackModelDescription.DATA_LAKE)
    private DataLakeV4Response dataLakeV4Response;

    @Schema(description = StackModelDescription.CLOUD_PLATFORM)
    private CloudPlatform cloudPlatform;

    @Schema(description = StackModelDescription.VARIANT)
    private String variant;

    @Schema(description = StackModelDescription.TUNNEL)
    private Tunnel tunnel = Tunnel.DIRECT;

    @Schema(description = StackModelDescription.FLOW_ID)
    private FlowIdentifier flowIdentifier;

    @Schema(description = StackModelDescription.EXTERNAL_DATABASE)
    private DatabaseResponse externalDatabase;

    @Schema(description = StackModelDescription.LOAD_BALANCER, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<LoadBalancerResponse> loadBalancers = new ArrayList<>();

    @Schema(description = StackModelDescription.ENABLE_LOAD_BALANCER, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableLoadBalancer;

    @Schema(description = StackModelDescription.JAVA_VERSION)
    private Integer javaVersion;

    @Schema(description = StackModelDescription.SUPPORTED_IMDS_VERSION)
    private String supportedImdsVersion;

    @Schema(description = ModelDescriptions.ARCHITECTURE)
    private String architecture;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableMultiAz;

    @Schema(description = StackModelDescription.ATTACHED_RESOURCES, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ResourceV4Response> resources = new ArrayList<>();

    @Schema(description = StackModelDescription.SECURITY)
    private SecurityV4Response security;

    @Schema(description = StackModelDescription.PROVIDER_SYNC_STATES)
    private Set<ProviderSyncState> providerSyncStates = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ClusterV4Response getCluster() {
        return cluster;
    }

    public void setCluster(ClusterV4Response cluster) {
        this.cluster = cluster;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public NetworkV4Response getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4Response network) {
        this.network = network;
    }

    public List<InstanceGroupV4Response> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupV4Response> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public StackImageV4Response getImage() {
        return image;
    }

    public void setImage(StackImageV4Response image) {
        this.image = image;
    }

    public CloudbreakDetailsV4Response getCloudbreakDetails() {
        return cloudbreakDetails;
    }

    public void setCloudbreakDetails(CloudbreakDetailsV4Response cloudbreakDetails) {
        this.cloudbreakDetails = cloudbreakDetails;
    }

    public StackAuthenticationV4Response getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationV4Response authentication) {
        this.authentication = authentication;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Set<HardwareInfoGroupV4Response> getHardwareInfoGroups() {
        return hardwareInfoGroups;
    }

    public void setHardwareInfoGroups(Set<HardwareInfoGroupV4Response> hardwareInfoGroups) {
        this.hardwareInfoGroups = hardwareInfoGroups;
    }

    public List<CloudbreakEventV4Response> getCloudbreakEvents() {
        return cloudbreakEvents;
    }

    public void setCloudbreakEvents(List<CloudbreakEventV4Response> cloudbreakEvents) {
        this.cloudbreakEvents = cloudbreakEvents;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public TagsV4Response getTags() {
        return tags;
    }

    public void setTags(TagsV4Response tags) {
        this.tags = tags;
    }

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(tags)
                .map(t -> t.getTagValue(key))
                .orElse(null);
    }

    public TelemetryResponse getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryResponse telemetry) {
        this.telemetry = telemetry;
    }

    public CustomDomainSettingsV4Response getCustomDomains() {
        return customDomains;
    }

    public void setCustomDomains(CustomDomainSettingsV4Response customDomains) {
        this.customDomains = customDomains;
    }

    public SharedServiceV4Response getSharedService() {
        return sharedService;
    }

    public void setSharedService(SharedServiceV4Response sharedService) {
        this.sharedService = sharedService;
    }

    public PlacementSettingsV4Response getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettingsV4Response placement) {
        this.placement = placement;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public String getCredentialCrn() {
        return credentialCrn;
    }

    public void setCredentialCrn(String credentialCrn) {
        this.credentialCrn = credentialCrn;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public DatabaseResponse getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(DatabaseResponse externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public List<LoadBalancerResponse> getLoadBalancers() {
        return loadBalancers;
    }

    public void setLoadBalancers(List<LoadBalancerResponse> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public boolean isEnableLoadBalancer() {
        return enableLoadBalancer;
    }

    public void setEnableLoadBalancer(boolean enableLoadBalancer) {
        this.enableLoadBalancer = enableLoadBalancer;
    }

    public boolean isGovCloud() {
        return govCloud;
    }

    public void setGovCloud(boolean govCloud) {
        this.govCloud = govCloud;
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

    public List<ResourceV4Response> getResources() {
        return resources;
    }

    public void setResources(List<ResourceV4Response> resources) {
        this.resources = resources;
    }

    public String getSupportedImdsVersion() {
        return supportedImdsVersion;
    }

    public void setSupportedImdsVersion(String supportedImdsVersion) {
        this.supportedImdsVersion = supportedImdsVersion;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public SecurityV4Response getSecurity() {
        return security;
    }

    public void setSecurity(SecurityV4Response security) {
        this.security = security;
    }

    public Set<ProviderSyncState> getProviderSyncStates() {
        return providerSyncStates;
    }

    public void setProviderSyncStates(Set<ProviderSyncState> providerSyncStates) {
        this.providerSyncStates = providerSyncStates;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public DataLakeV4Response getDataLakeResponse() {
        return dataLakeV4Response;
    }

    public void setDataLakeResponse(DataLakeV4Response dataLakeV4Response) {
        this.dataLakeV4Response = dataLakeV4Response;
    }

    @Override
    public String toString() {
        return "StackV4Response{ " +
                super.toString() +
                " id=" + id +
                ", crn='" + crn + '\'' +
                ", govCloud='" + govCloud + '\'' +
                ", region='" + region + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", environmentName='" + environmentName + '\'' +
                ", environmentType='" + environmentType + '\'' +
                ", credentialName='" + credentialName + '\'' +
                ", credentialCrn='" + credentialCrn + '\'' +
                ", status=" + status +
                ", cluster=" + cluster +
                ", statusReason='" + statusReason + '\'' +
                ", network=" + network +
                ", instanceGroups=" + instanceGroups +
                ", created=" + created +
                ", terminated=" + terminated +
                ", gatewayPort=" + gatewayPort +
                ", image=" + image +
                ", cloudbreakDetails=" + cloudbreakDetails +
                ", authentication=" + authentication +
                ", nodeCount=" + nodeCount +
                ", hardwareInfoGroups=" + hardwareInfoGroups +
                ", cloudbreakEvents=" + cloudbreakEvents +
                ", tags=" + tags +
                ", telemetry=" + telemetry +
                ", workspace=" + workspace +
                ", customDomains=" + customDomains +
                ", placement=" + placement +
                ", sharedService=" + sharedService +
                ", cloudPlatform=" + cloudPlatform +
                ", variant=" + variant +
                ", tunnel=" + tunnel +
                ", flowIdentifier=" + flowIdentifier +
                ", externalDatabase=" + externalDatabase +
                ", javaVersion=" + javaVersion +
                ", multiAz=" + enableMultiAz +
                ", resources=" + resources +
                ", supportedImdsVersion=" + supportedImdsVersion +
                ", architecture=" + architecture +
                ", security=" + security +
                ", providerSyncStates=" + providerSyncStates +
                ", dataLakeV4Response=" + dataLakeV4Response +
                '}';
    }
}
