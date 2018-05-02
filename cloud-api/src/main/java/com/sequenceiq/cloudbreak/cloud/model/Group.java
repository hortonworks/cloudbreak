package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class Group extends DynamicModel {

    private final String name;

    private final InstanceGroupType type;

    private final List<CloudInstance> instances;

    private final Security security;

    private final String publicKey;

    private final String loginUserName;

    private final InstanceAuthentication instanceAuthentication;

    private final Optional<CloudInstance> skeleton;

    private final int rootVolumeSize;

    public Group(String name, InstanceGroupType type, Collection<CloudInstance> instances, Security security, CloudInstance skeleton,
            InstanceAuthentication instanceAuthentication, String loginUserName, String publicKey, int rootVolumeSize) {
        this.name = name;
        this.type = type;
        this.instances = ImmutableList.copyOf(instances);
        this.security = security;
        this.skeleton = Optional.ofNullable(skeleton);
        this.instanceAuthentication = instanceAuthentication;
        this.publicKey = publicKey;
        this.loginUserName = loginUserName;
        this.rootVolumeSize = rootVolumeSize;
    }

    public Group(String name, InstanceGroupType type, Collection<CloudInstance> instances, Security security, CloudInstance skeleton,
            Map<String, Object> parameters, InstanceAuthentication instanceAuthentication, String loginUserName, String publicKey, int rootVolumeSize) {
        super(parameters);
        this.name = name;
        this.type = type;
        this.instances = ImmutableList.copyOf(instances);
        this.security = security;
        this.skeleton = Optional.ofNullable(skeleton);
        this.instanceAuthentication = instanceAuthentication;
        this.publicKey = publicKey;
        this.loginUserName = loginUserName;
        this.rootVolumeSize = rootVolumeSize;
    }

    public CloudInstance getReferenceInstanceConfiguration() {
        if (instances.isEmpty()) {
            return skeleton.get();
        }
        return instances.get(0);
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

    @Override
    public String toString() {
        return "Group{"
                + "name='" + name + '\''
                + ", type=" + type
                + ", instances=" + instances
                + ", security=" + security
                + ", publicKey='" + publicKey + '\''
                + ", loginUserName='" + loginUserName + '\''
                + ", instanceAuthentication=" + instanceAuthentication
                + ", skeleton=" + skeleton
                + '}';
    }
}
