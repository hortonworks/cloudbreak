package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;


@Entity
@NamedQueries({
        @NamedQuery(name = "InstanceGroup.findOneByGroupNameInStack",
                query = "SELECT i from InstanceGroup i "
                        + "WHERE i.stack.id = :stackId "
                        + "AND i.groupName = :groupName")
})
public class InstanceGroup implements ProvisionEntity, Comparable<InstanceGroup> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroup_generator")
    @SequenceGenerator(name = "instancegroup_generator", sequenceName = "instancegroup_id_seq", allocationSize = 1)
    private Long id;
    @OneToOne
    private Template template;
    private Integer nodeCount;
    private String groupName;
    @Enumerated(EnumType.STRING)
    private InstanceGroupType instanceGroupType = InstanceGroupType.CORE;
    @ManyToOne
    private Stack stack;
    @OneToMany(mappedBy = "instanceGroup", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<InstanceMetaData> instanceMetaData = new HashSet<>();

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

    public Integer getNodeCount() {
        return nodeCount;
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

    public Set<InstanceMetaData> getInstanceMetaData() {
        Set<InstanceMetaData> resultSet = new HashSet<>();
        for (InstanceMetaData metaData : instanceMetaData) {
            if (!metaData.isTerminated()) {
                resultSet.add(metaData);
            }
        }
        return resultSet;
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

    @Override
    public int compareTo(InstanceGroup o) {
        return this.groupName.compareTo(o.groupName);
    }
}
