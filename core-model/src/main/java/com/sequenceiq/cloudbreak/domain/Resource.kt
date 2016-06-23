package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.common.type.CommonStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Entity
@NamedQueries(@NamedQuery(
        name = "Resource.findByStackIdAndNameAndType",
        query = "SELECT r FROM Resource r "
                + "WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type"), @NamedQuery(
        name = "Resource.findByStackIdAndResourceNameOrReference",
        query = "SELECT r FROM Resource r "
                + "WHERE r.stack.id = :stackId AND (r.resourceName = :resource OR r.resourceReference = :resource)"))
class Resource : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "resource_generator")
    @SequenceGenerator(name = "resource_generator", sequenceName = "resource_id_seq", allocationSize = 1)
    var id: Long? = null
    var instanceGroup: String? = null

    @Enumerated(EnumType.STRING)
    var resourceType: ResourceType? = null
    @Enumerated(EnumType.STRING)
    var resourceStatus: CommonStatus? = null

    var resourceName: String? = null
    var resourceReference: String? = null

    @ManyToOne
    @JoinColumn(name = "resource_stack")
    var stack: Stack? = null

    constructor() {

    }

    constructor(resourceType: ResourceType, resourceName: String, stack: Stack) : this(resourceType, resourceName, null, CommonStatus.CREATED, stack, null) {
    }

    constructor(resourceType: ResourceType, resourceName: String, stack: Stack, instanceGroup: String) : this(resourceType, resourceName, null, CommonStatus.CREATED, stack, instanceGroup) {
    }

    constructor(resourceType: ResourceType, resourceName: String, resourceReference: String?, status: CommonStatus, stack: Stack, instanceGroup: String?) {
        this.resourceType = resourceType
        this.resourceName = resourceName
        this.resourceReference = resourceReference
        this.resourceStatus = status
        this.instanceGroup = instanceGroup
        this.stack = stack
    }
}
