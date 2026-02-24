package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.security.SecurityV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXV1Request extends DistroXV1Base implements TaggableRequest {

    private String environmentName;

    @Valid
    private Set<InstanceGroupV1Request> instanceGroups;

    private DistroXImageV1Request image;

    private NetworkV1Request network;

    @Valid
    private DistroXClusterV1Request cluster;

    private SdxV1Request sdx;

    @Valid
    private DistroXDatabaseRequest externalDatabase;

    private TagsV1Request tags;

    private Map<String, Object> inputs = new HashMap<>();

    @Schema(hidden = true)
    private Integer gatewayPort;

    private boolean enableLoadBalancer;

    private String variant;

    private Integer javaVersion;

    private boolean enableMultiAz;

    @Schema(description = ModelDescriptions.ARCHITECTURE)
    private String architecture;

    @Schema(description = ModelDescriptions.Database.DISABLE_DB_SSL_ENFORCEMENT)
    private boolean disableDbSslEnforcement;

    @Schema(description = ModelDescriptions.NOTIFICATION_STATE)
    private NotificationState notificationState;

    @Schema(description = ModelDescriptions.StackModelDescription.SECURITY)
    private SecurityV1Request security;

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public Set<InstanceGroupV1Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupV1Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public DistroXImageV1Request getImage() {
        return image;
    }

    public void setImage(DistroXImageV1Request image) {
        this.image = image;
    }

    public NetworkV1Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV1Request network) {
        this.network = network;
    }

    public DistroXClusterV1Request getCluster() {
        return cluster;
    }

    public void setCluster(DistroXClusterV1Request cluster) {
        this.cluster = cluster;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public SdxV1Request getSdx() {
        return sdx;
    }

    public void setSdx(SdxV1Request sdx) {
        this.sdx = sdx;
    }

    public DistroXDatabaseRequest getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(DistroXDatabaseRequest externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    public TagsV1Request getTags() {
        return tags;
    }

    public TagsV1Request initAndGetTags() {
        if (tags == null) {
            tags = new TagsV1Request();
        }
        return tags;
    }

    @Override
    public void addTag(String key, String value) {
        initAndGetTags().getUserDefined().put(key, value);
    }

    public void setTags(TagsV1Request tags) {
        this.tags = tags;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer port) {
        gatewayPort = port;
    }

    public boolean isEnableLoadBalancer() {
        return enableLoadBalancer;
    }

    public void setEnableLoadBalancer(boolean enableLoadBalancer) {
        this.enableLoadBalancer = enableLoadBalancer;
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

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public boolean isDisableDbSslEnforcement() {
        return disableDbSslEnforcement;
    }

    public void setDisableDbSslEnforcement(boolean disableDbSslEnforcement) {
        this.disableDbSslEnforcement = disableDbSslEnforcement;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationState = notificationState;
    }

    public SecurityV1Request getSecurity() {
        return security;
    }

    public void setSecurity(SecurityV1Request security) {
        this.security = security;
    }

    @JsonIgnore
    public Set<String> getAllRecipes() {
        Set<String> recipes = Sets.newHashSet();
        if (instanceGroups != null) {
            instanceGroups.stream()
                    .filter(Objects::nonNull)
                    .map(InstanceGroupV1Request::getRecipeNames)
                    .filter(Objects::nonNull)
                    .forEach(recipeNames -> recipes.addAll(recipeNames
                            .stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet())
                    ));
        }
        return recipes;
    }
}
