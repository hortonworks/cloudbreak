package com.sequenceiq.cloudbreak.domain

import java.util.Date

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

@Entity
class CloudbreakUsage : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cloudbreakusage_generator")
    @SequenceGenerator(name = "cloudbreakusage_generator", sequenceName = "cloudbreakusage_id_seq", allocationSize = 1)
    var id: Long? = null

    var owner: String? = null

    var account: String? = null

    var stackId: Long? = null

    var stackName: String? = null

    var provider: String? = null

    var region: String? = null

    var availabilityZone: String? = null

    var day: Date? = null

    var instanceHours: Long? = null

    var costs: Double? = null

    var instanceType: String? = null

    var instanceGroup: String? = null

    override fun toString(): String {
        val sb = StringBuilder("CloudbreakUsage{")
        sb.append("id=").append(id)
        sb.append(", owner='").append(owner).append('\'')
        sb.append(", account='").append(account).append('\'')
        sb.append(", day=").append(day)
        sb.append(", provider='").append(provider).append('\'')
        sb.append(", region='").append(region).append('\'')
        sb.append(", availabilityZone='").append(availabilityZone).append('\'')
        sb.append(", instanceHours='").append(instanceHours).append('\'')
        sb.append(", stackId='").append(stackId).append('\'')
        sb.append(", stackName='").append(stackName).append('\'')
        sb.append(", instanceType='").append(instanceType).append('\'')
        sb.append(", instanceGroup='").append(instanceGroup).append('\'')
        sb.append(", costs='").append(costs).append('\'')
        sb.append('}')
        return sb.toString()
    }

}
