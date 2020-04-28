package com.sequenceiq.cloudbreak.domain.stack.instance;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.converter.ScalingModeConverter;
import com.sequenceiq.common.api.type.ScalingMode;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.CloudIdentityType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@NamedEntityGraph(name = "InstanceGroup.instanceMetaData",
        attributeNodes = @NamedAttributeNode("instanceMetaData"))
@Entity
public class InstanceGroup implements ProvisionEntity, Comparable<InstanceGroup> {

    public static final String IDENTITY_TYPE_ATTRIBUTE = "identityType";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroup_generator")
    @SequenceGenerator(name = "instancegroup_generator", sequenceName = "instancegroup_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Template template;

    @OneToOne
    private SecurityGroup securityGroup;

    private String groupName;

    @Enumerated(EnumType.STRING)
    private InstanceGroupType instanceGroupType = InstanceGroupType.CORE;

    @Convert(converter = ScalingModeConverter.class)
    private ScalingMode scalingMode = ScalingMode.UNSPECIFIED;

    @ManyToOne
    private Stack stack;

    @OneToMany(mappedBy = "instanceGroup", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<InstanceMetaData> instanceMetaData = new HashSet<>();

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public int getNodeCount() {
        return getNotTerminatedInstanceMetaDataSet().size();
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public Set<InstanceMetaData> getNotTerminatedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotDeletedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getReachableInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(InstanceMetaData::isReachable)
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getUnattachedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> metaData.getInstanceStatus() == InstanceStatus.CREATED)
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getAttachedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(InstanceMetaData::isAttached)
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getRunningInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(InstanceMetaData::isRunning)
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getInstanceMetaDataSet() {
        return instanceMetaData;
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public ScalingMode getScalingMode() {
        return scalingMode;
    }

    public void setScalingMode(ScalingMode scalingMode) {
        this.scalingMode = scalingMode;
    }

    public Set<InstanceMetaData> getAllInstanceMetaData() {
        return instanceMetaData;
    }

    public void setInstanceMetaData(Set<InstanceMetaData> instanceMetaData) {
        this.instanceMetaData = instanceMetaData;
    }

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public void setInstanceGroupType(InstanceGroupType instanceGroupType) {
        this.instanceGroupType = instanceGroupType;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public Optional<CloudIdentityType> getCloudIdentityType() {
        if (attributes != null && StringUtils.isNotEmpty(attributes.getValue())) {
            Map<String, Object> attributeMap = attributes.getMap();
            if (attributeMap.containsKey(IDENTITY_TYPE_ATTRIBUTE)) {
                return Optional.of(CloudIdentityType.valueOf(attributeMap.get(IDENTITY_TYPE_ATTRIBUTE).toString()));
            }
        }
        return Optional.empty();
    }

    public void setCloudIdentityType(CloudIdentityType cloudIdentityType) {
        Map<String, Object> attributeMap = attributes.getMap();
        attributeMap.put(InstanceGroup.IDENTITY_TYPE_ATTRIBUTE, cloudIdentityType);
        attributes = new Json(attributeMap);
    }

    @SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
    @Override
    public int compareTo(InstanceGroup o) {
        return groupName.compareTo(o.groupName);
    }
}
