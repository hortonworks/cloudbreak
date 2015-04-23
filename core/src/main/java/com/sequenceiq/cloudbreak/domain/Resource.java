package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "Resource.findByStackIdAndName",
                query = "SELECT r FROM Resource r "
                        + "WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
})
public class Resource {

    @Id
    @GeneratedValue
    private Long id;
    private String instanceGroup;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    private String resourceName;

    @ManyToOne
    @JoinColumn(name = "resource_stack")
    private Stack stack;

    public Resource() {

    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack) {
        this(resourceType, resourceName, stack, null);
    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack, String instanceGroup) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.instanceGroup = instanceGroup;
        this.stack = stack;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }
}
