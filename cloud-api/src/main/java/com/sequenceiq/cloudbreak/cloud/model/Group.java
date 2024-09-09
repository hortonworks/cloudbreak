package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private String rootVolumeType;

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
            @JsonProperty("tags") Map<String, String> tags,
            @JsonProperty("rootVolumeType") String rootVolumeType) {
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
        this.rootVolumeType = rootVolumeType;
    }

    public Group(Builder builder) {
        super(builder.parameters);
        this.name = builder.name;
        this.type = builder.type;
        builder.instances.forEach(Objects::requireNonNull);
        this.instances = new ArrayList<>(builder.instances);
        this.security = builder.security;
        this.publicKey = builder.publicKey;
        this.loginUserName = builder.loginUserName;
        this.instanceAuthentication = builder.instanceAuthentication;
        this.skeleton = Optional.ofNullable(builder.skeleton);
        this.rootVolumeSize = builder.rootVolumeSize;
        this.identity = builder.identity;
        this.deletedInstances = new ArrayList<>(builder.deletedInstances);
        this.network = builder.network;
        this.tags = builder.tags;
        this.rootVolumeType = builder.rootVolumeType;
    }

    public CloudInstance getReferenceInstanceConfiguration() {
        if (instances.isEmpty()) {
            return skeleton.orElseThrow(() -> new RuntimeException(String.format("There is no skeleton and instance available for Group -> name:%s", name)));
        }
        return instances.getFirst();
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

    public String getRootVolumeType() {
        return rootVolumeType;
    }

    public void setRootVolumeType(String rootVolumeType) {
        this.rootVolumeType = rootVolumeType;
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
                ", rootVolumeType=" + rootVolumeType +
                ", identity=" + identity +
                ", deletedInstances=" + deletedInstances +
                ", network=" + network +
                ", tags=" + tags +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Map<String, String> tags = Collections.emptyMap();

        private GroupNetwork network;

        private Collection<CloudInstance> deletedInstances = Collections.emptyList();

        private Optional<CloudFileSystemView> identity = Optional.empty();

        private int rootVolumeSize;

        private CloudInstance skeleton;

        private InstanceAuthentication instanceAuthentication;

        private String loginUserName;

        private String publicKey;

        private Security security;

        private Collection<CloudInstance> instances = Collections.emptyList();

        private InstanceGroupType type;

        private String name;

        private Map<String, Object> parameters = Collections.emptyMap();

        private String rootVolumeType;

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withNetwork(GroupNetwork network) {
            this.network = network;
            return this;
        }

        public Builder withDeletedInstances(List<CloudInstance> deletedInstances) {
            this.deletedInstances = deletedInstances;
            return this;
        }

        public Builder withIdentity(Optional<CloudFileSystemView> identity) {
            this.identity = identity;
            return this;
        }

        public Builder withRootVolumeSize(int rootVolumeSize) {
            this.rootVolumeSize = rootVolumeSize;
            return this;
        }

        public Builder withSkeleton(CloudInstance skeleton) {
            this.skeleton = skeleton;
            return this;
        }

        public Builder withInstanceAuthentication(InstanceAuthentication instanceAuthentication) {
            this.instanceAuthentication = instanceAuthentication;
            return this;
        }

        public Builder withLoginUserName(String loginUserName) {
            this.loginUserName = loginUserName;
            return this;
        }

        public Builder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder withSecurity(Security security) {
            this.security = security;
            return this;
        }

        public Builder withInstances(Collection<CloudInstance> instances) {
            this.instances = instances;
            return this;
        }

        public Builder withType(InstanceGroupType type) {
            this.type = type;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder withRootVolumeType(String rootVolumeType) {
            this.rootVolumeType = rootVolumeType;
            return this;
        }

        public Group build() {
            return new Group(this);
        }
    }
}
