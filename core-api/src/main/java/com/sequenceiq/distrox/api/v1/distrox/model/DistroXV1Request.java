package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.authorization.api.EnvironmentNameAwareApiModel;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DistroXV1Request extends DistroXV1Base implements EnvironmentNameAwareApiModel {

    private String environmentName;

    private Set<InstanceGroupV1Request> instanceGroups;

    private DistroXImageV1Request image;

    private NetworkV1Request network;

    @Valid
    private DistroXClusterV1Request cluster;

    private SdxV1Request sdx;

    private TagsV1Request tags;

    private Map<String, Object> inputs = new HashMap<>();

    @ApiModelProperty(hidden = true)
    private Integer gatewayPort;

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

    public TagsV1Request getTags() {
        return tags;
    }

    public TagsV1Request initAndGetTags() {
        if (tags == null) {
            tags = new TagsV1Request();
        }
        return tags;
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
}
