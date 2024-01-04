package com.sequenceiq.freeipa.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.entity.util.InstanceGroupTypeConverter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@NamedEntityGraph(name = "InstanceGroup.instanceMetaData",
        attributeNodes = @NamedAttributeNode("instanceMetaData"))
@Entity
public class InstanceGroup implements Comparable<InstanceGroup> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroup_generator")
    @SequenceGenerator(name = "instancegroup_generator", sequenceName = "instancegroup_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Template template;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SecurityGroup securityGroup;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "availabilityzone")
    private Set<String> availabilityZones = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private InstanceGroupNetwork instanceGroupNetwork;

    private String groupName;

    @Convert(converter = InstanceGroupTypeConverter.class)
    private InstanceGroupType instanceGroupType = InstanceGroupType.MASTER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private Stack stack;

    @OneToMany(mappedBy = "instanceGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<InstanceMetaData> instanceMetaData = new HashSet<>();

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    private Integer nodeCount;

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

    public int getNodeCount() {
        return nodeCount == null ? getNotTerminatedInstanceMetaDataSet().size() : nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public InstanceGroupNetwork getInstanceGroupNetwork() {
        return instanceGroupNetwork;
    }

    public void setInstanceGroupNetwork(InstanceGroupNetwork instanceGroupNetwork) {
        this.instanceGroupNetwork = instanceGroupNetwork;
    }

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Set<String> availabilityZones) {
        this.availabilityZones = availabilityZones;
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

    public Set<InstanceMetaData> getInstanceMetaDataSet() {
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

    @SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
    @Override
    public int compareTo(InstanceGroup o) {
        return groupName.compareTo(o.groupName);
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Set<InstanceMetaData> getInstanceMetaData() {
        return instanceMetaData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            InstanceGroup that = (InstanceGroup) o;
            return Objects.equals(id, that.id)
                    && Objects.equals(template, that.template)
                    && Objects.equals(instanceGroupNetwork, that.instanceGroupNetwork)
                    && Objects.equals(groupName, that.groupName)
                    && instanceGroupType == that.instanceGroupType
                    && Objects.equals(attributes, that.attributes)
                    && Objects.equals(nodeCount, that.nodeCount);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, template, instanceGroupNetwork, groupName, instanceGroupType, attributes, nodeCount);
    }
}
