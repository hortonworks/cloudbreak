package com.sequenceiq.cloudbreak.domain.stack.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.InstanceGroupTypeConverter;
import com.sequenceiq.cloudbreak.converter.ScalabilityOptionConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ScalabilityOption;
import com.sequenceiq.common.model.CloudIdentityType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@NamedEntityGraph(name = "InstanceGroup.instanceMetaData",
        attributeNodes = @NamedAttributeNode("instanceMetaData"))
@Entity
public class InstanceGroup implements ProvisionEntity, Comparable<InstanceGroup>, InstanceGroupView {

    public static final String IDENTITY_TYPE_ATTRIBUTE = "identityType";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroup_generator")
    @SequenceGenerator(name = "instancegroup_generator", sequenceName = "instancegroup_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Template template;

    @OneToOne(fetch = FetchType.LAZY)
    private SecurityGroup securityGroup;

    private String groupName;

    @Convert(converter = InstanceGroupTypeConverter.class)
    private InstanceGroupType instanceGroupType = InstanceGroupType.CORE;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stack stack;

    @OneToMany(mappedBy = "instanceGroup", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<InstanceMetaData> instanceMetaData = new HashSet<>();

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    private int initialNodeCount;

    private int minimumNodeCount;

    @Convert(converter = ScalabilityOptionConverter.class)
    private ScalabilityOption scalabilityOption = ScalabilityOption.ALLOWED;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name = "targetgroup_instancegroup",
            joinColumns = @JoinColumn(name = "instancegroups_id"),
            inverseJoinColumns = @JoinColumn(name = "targetgroups_id")
    )
    private Set<TargetGroup> targetGroups = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "instanceGroup")
    private Set<AvailabilityZone> availabilityZones = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    private InstanceGroupNetwork instanceGroupNetwork;

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

    public int getMinimumNodeCount() {
        return minimumNodeCount;
    }

    public void setMinimumNodeCount(int minimumNodeCount) {
        this.minimumNodeCount = minimumNodeCount;
    }

    public InstanceGroup replaceInstanceMetadata(Set<InstanceMetaData> instanceMetaData) {
        this.instanceMetaData.clear();
        Optional.ofNullable(instanceMetaData).ifPresent(this.instanceMetaData::addAll);
        return this;
    }

    public Set<InstanceMetaData> getNotTerminatedAndNotZombieInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isZombie())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotTerminatedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotDeletedAndNotZombieInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider() && !metaData.isZombie())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotDeletedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getZombieInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> metaData.isZombie())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getDeletedInstanceMetaDataSet() {
        return instanceMetaData.stream()
            .filter(metaData -> metaData.isTerminated() || metaData.isDeletedOnProvider())
            .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getReachableInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(InstanceMetaData::isReachable)
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getUnattachedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> metaData.getInstanceStatus() == InstanceStatus.CREATED || metaData.getInstanceStatus() == InstanceStatus.DECOMMISSIONED)
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

    public Set<InstanceMetaData> getInstanceMetaData() {
        return instanceMetaData;
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
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

    public int getInitialNodeCount() {
        return initialNodeCount;
    }

    public void setInitialNodeCount(int initialNodeCount) {
        this.initialNodeCount = initialNodeCount;
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

    public Set<TargetGroup> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(Set<TargetGroup> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public void addTargetGroup(TargetGroup targetGroup) {
        targetGroups.add(targetGroup);
    }

    @SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
    @Override
    public int compareTo(InstanceGroup o) {
        return groupName.compareTo(o.groupName);
    }

    public ScalabilityOption getScalabilityOption() {
        return scalabilityOption;
    }

    public void setScalabilityOption(ScalabilityOption scalabilityOption) {
        this.scalabilityOption = scalabilityOption;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones.stream().map(AvailabilityZone::getAvailabilityZone).collect(Collectors.toSet());
    }

    public void setAvailabilityZones(Set<AvailabilityZone> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public InstanceGroupNetwork getInstanceGroupNetwork() {
        return instanceGroupNetwork;
    }

    public void setInstanceGroupNetwork(InstanceGroupNetwork instanceGroupNetwork) {
        this.instanceGroupNetwork = instanceGroupNetwork;
    }

    @Override
    public String toString() {
        return "InstanceGroup{" +
                "id=" + id +
                ", groupName='" + groupName + '\'' +
                ", instanceGroupType=" + instanceGroupType +
                ", stack=" + stack.getName() +
                '}';
    }

    public List<InstanceMetadataView> getAllAvailableInstanceMetadata() {
        return new ArrayList<>(getNotTerminatedAndNotZombieInstanceMetaDataSet());
    }
}
