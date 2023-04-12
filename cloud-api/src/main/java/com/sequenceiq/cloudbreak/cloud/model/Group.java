package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class Group extends DynamicModel {

    private final String name;

    private final InstanceGroupType type;

    private final List<CloudInstance> instances;

    private final Security security;

    private final String publicKey;

    private final String loginUserName;

    private final InstanceAuthentication instanceAuthentication;

    private final Optional<CloudInstance> skeleton;

    private int rootVolumeSize;

    private final Optional<CloudFileSystemView> identity;

    private final List<CloudInstance> deletedInstances;

    private final GroupNetwork network;

    private final Map<String, String> tags;

    public Group(String name,
            InstanceGroupType type,
            Collection<CloudInstance> instances,
            Security security,
            CloudInstance skeleton,
            InstanceAuthentication instanceAuthentication,
            String loginUserName,
            String publicKey,
            int rootVolumeSize,
            Optional<CloudFileSystemView> identity,
            GroupNetwork groupNetwork,
            Map<String, String> tags) {
        this(name, type, instances, security, skeleton, new HashMap<>(), instanceAuthentication, loginUserName, publicKey,
                rootVolumeSize, identity, new ArrayList<>(), groupNetwork, tags);
    }

    public Group(String name,
            InstanceGroupType type,
            Collection<CloudInstance> instances,
            Security security,
            CloudInstance skeleton,
            InstanceAuthentication instanceAuthentication,
            String loginUserName,
            String publicKey,
            int rootVolumeSize,
            Optional<CloudFileSystemView> identity,
            List<CloudInstance> deletedInstances,
            GroupNetwork groupNetwork,
            Map<String, String> tags) {
        this(name, type, instances, security, skeleton, new HashMap<>(), instanceAuthentication, loginUserName, publicKey,
                rootVolumeSize, identity, deletedInstances, groupNetwork, tags);
    }

    public Group(String name,
            InstanceGroupType type,
            Collection<CloudInstance> instances,
            Security security,
            CloudInstance skeleton,
            Map<String, Object> parameters,
            InstanceAuthentication instanceAuthentication,
            String loginUserName,
            String publicKey,
            int rootVolumeSize,
            Optional<CloudFileSystemView> identity,
            GroupNetwork groupNetwork,
            Map<String, String> tags) {
        this(name, type, instances, security, skeleton, parameters, instanceAuthentication, loginUserName, publicKey,
                rootVolumeSize, identity, new ArrayList<>(), groupNetwork, tags);
    }

    @JsonCreator
    public Group(
            @JsonProperty("name") String name,
            @JsonProperty("type") InstanceGroupType type,
            @JsonProperty("instances") Collection<CloudInstance> instances,
            @JsonProperty("security") Security security,
            @JsonProperty("skeleton") CloudInstance skeleton,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("instanceAuthentication") InstanceAuthentication instanceAuthentication,
            @JsonProperty("loginUserName") String loginUserName,
            @JsonProperty("publicKey") String publicKey,
            @JsonProperty("rootVolumeSize") int rootVolumeSize,
            @JsonProperty("identity") Optional<CloudFileSystemView> identity,
            @JsonProperty("deletedInstances") List<CloudInstance> deletedInstances,
            @JsonProperty("network") GroupNetwork groupNetwork,
            @JsonProperty("tags") Map<String, String> tags) {
        super(parameters);
        this.name = name;
        this.type = type;
        instances.forEach(Objects::requireNonNull);
        this.instances = new ArrayList<>(instances);
        this.security = security;
        this.skeleton = Optional.ofNullable(skeleton);
        this.instanceAuthentication = instanceAuthentication;
        this.publicKey = publicKey;
        this.loginUserName = loginUserName;
        this.rootVolumeSize = rootVolumeSize;
        this.identity = identity;
        this.deletedInstances = deletedInstances;
        this.network = groupNetwork;
        this.tags = tags;
    }

    public CloudInstance getReferenceInstanceConfiguration() {
        if (instances.isEmpty()) {
            return skeleton.orElseThrow(() -> new RuntimeException(String.format("There is no skeleton and instance available for Group -> name:%s", name)));
        }
        return instances.get(0);
    }

    /**
     * Need this for Jackson serialization
     */
    private CloudInstance getSkeleton() {
        return getReferenceInstanceConfiguration();
    }

    public InstanceTemplate getReferenceInstanceTemplate() {
        return getReferenceInstanceConfiguration().getTemplate();
    }

    public String getName() {
        return name;
    }

    public InstanceGroupType getType() {
        return type;
    }

    public List<CloudInstance> getInstances() {
        return instances;
    }

    public Integer getInstancesSize() {
        return instances.size();
    }

    public Security getSecurity() {
        return security;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public InstanceAuthentication getInstanceAuthentication() {
        return instanceAuthentication;
    }

    public int getRootVolumeSize() {
        return rootVolumeSize;
    }

    public Optional<CloudFileSystemView> getIdentity() {
        return identity;
    }

    public List<CloudInstance> getDeletedInstances() {
        return deletedInstances;
    }

    public GroupNetwork getNetwork() {
        return network;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setRootVolumeSize(int rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }

    @Override
    public String toString() {
        return "Group{" +
                "name=" + name +
                ", type=" + type +
                ", instances=" + instances +
                ", security=" + security +
                ", publicKey=" + publicKey +
                ", loginUserName=" + loginUserName +
                ", instanceAuthentication=" + instanceAuthentication +
                ", skeleton=" + skeleton +
                ", rootVolumeSize=" + rootVolumeSize +
                ", identity=" + identity +
                ", deletedInstances=" + deletedInstances +
                ", network=" + network +
                ", tags=" + tags +
                '}';
    }
}
