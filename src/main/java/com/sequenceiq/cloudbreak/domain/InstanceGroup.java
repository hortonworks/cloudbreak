package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class InstanceGroup implements ProvisionEntity, Comparable {
    @Id
    @GeneratedValue
    private Long id;
    @OneToOne
    private Template template;
    private Integer nodeCount;
    private String groupName;
    @ManyToOne
    private Stack stack;
    @OneToMany(mappedBy = "instanceGroup", cascade = CascadeType.ALL, orphanRemoval = true)
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

    @Override
    public int compareTo(Object o) {
        return Integer.compare(getNodeCount(), ((InstanceGroup) o).getNodeCount());
    }
}
