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
//@Table(uniqueConstraints = {
//        @UniqueConstraint(columnNames = { "resourceType", "resourceName", "resource_stack" })
//})
@NamedQueries({
        @NamedQuery(
                name = "Resource.findByStackIdAndNameAndType",
                query = "SELECT r FROM Resource r "
                        + "WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
})
public class Resource implements ProvisionEntity {
    public enum Status {
        REQUESTED,
        CREATED
    }

    @Id
    @GeneratedValue
    private Long id;
    private String instanceGroup;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
    @Enumerated(EnumType.STRING)
    private Resource.Status resourceStatus;

    private String resourceName;
    private String resourceReference;

    @ManyToOne
    @JoinColumn(name = "resource_stack")
    private Stack stack;

    public Resource() {

    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack) {
        this(resourceType, resourceName, null, Status.CREATED, stack, null);
    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack, String instanceGroup) {
        this(resourceType, resourceName, null, Status.CREATED, stack, instanceGroup);
    }

    public Resource(ResourceType resourceType, String resourceName, String resourceReference, Status status, Stack stack, String instanceGroup) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.resourceReference = resourceReference;
        this.resourceStatus = status;
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

    public Status getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(Status resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public String getResourceReference() {
        return resourceReference;
    }

    public void setResourceReference(String resourceReference) {
        this.resourceReference = resourceReference;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }
}
