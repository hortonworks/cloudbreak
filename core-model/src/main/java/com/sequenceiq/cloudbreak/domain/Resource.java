package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "Resource.findByStackIdAndNameAndType",
                query = "SELECT r FROM Resource r "
                        + "WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type"),
        @NamedQuery(
                name = "Resource.findByStackIdAndResourceNameOrReference",
                query = "SELECT r FROM Resource r "
                        + "WHERE r.stack.id = :stackId AND (r.resourceName = :resource OR r.resourceReference = :resource)")
})
public class Resource implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "resource_generator")
    @SequenceGenerator(name = "resource_generator", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;
    private String instanceGroup;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
    @Enumerated(EnumType.STRING)
    private CommonStatus resourceStatus;

    private String resourceName;
    private String resourceReference;

    @ManyToOne
    @JoinColumn(name = "resource_stack")
    private Stack stack;

    public Resource() {

    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, stack, null);
    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack, String instanceGroup) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, stack, instanceGroup);
    }

    public Resource(ResourceType resourceType, String resourceName, String resourceReference, CommonStatus status, Stack stack, String instanceGroup) {
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

    public CommonStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(CommonStatus resourceStatus) {
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
