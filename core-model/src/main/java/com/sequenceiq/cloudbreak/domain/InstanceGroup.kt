package com.sequenceiq.cloudbreak.domain

import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType

@Entity
@NamedQueries(@NamedQuery(name = "InstanceGroup.findOneByGroupNameInStack",
        query = "SELECT i from InstanceGroup i "
                + "WHERE i.stack.id = :stackId "
                + "AND i.groupName = :groupName"))
class InstanceGroup : ProvisionEntity, Comparable<InstanceGroup> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroup_generator")
    @SequenceGenerator(name = "instancegroup_generator", sequenceName = "instancegroup_id_seq", allocationSize = 1)
    var id: Long? = null
    @OneToOne
    var template: Template? = null
    var nodeCount: Int? = null
    var groupName: String? = null
    @Enumerated(EnumType.STRING)
    var instanceGroupType = InstanceGroupType.CORE
    @ManyToOne
    var stack: Stack? = null
    @OneToMany(mappedBy = "instanceGroup", cascade = CascadeType.REMOVE, orphanRemoval = true)
    var instanceMetaData: Set<InstanceMetaData> = HashSet()
        get() {
            val resultSet = HashSet<InstanceMetaData>()
            for (metaData in instanceMetaData) {
                if (!metaData.isTerminated) {
                    resultSet.add(metaData)
                }
            }
            return resultSet
        }

    val allInstanceMetaData: Set<InstanceMetaData>
        get() = instanceMetaData

    override fun compareTo(o: InstanceGroup): Int {
        return this.groupName!!.compareTo(o.groupName)
    }
}
