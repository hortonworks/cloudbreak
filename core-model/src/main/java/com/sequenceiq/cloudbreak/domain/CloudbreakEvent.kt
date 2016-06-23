package com.sequenceiq.cloudbreak.domain

import java.util.Date

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator
import javax.persistence.Table

import com.sequenceiq.cloudbreak.api.model.Status

@Entity
@NamedQueries(@NamedQuery(
        name = "CloudbreakEvent.cloudbreakEvents",
        query = "SELECT cbe FROM CloudbreakEvent cbe "
                + "WHERE cbe.owner= :owner ORDER BY cbe.eventTimestamp ASC"), @NamedQuery(
        name = "CloudbreakEvent.cloudbreakEventsSince",
        query = "SELECT cbe FROM CloudbreakEvent cbe "
                + "WHERE cbe.owner= :owner AND cbe.eventTimestamp > :since "
                + "ORDER BY cbe.eventTimestamp ASC"), @NamedQuery(
        name = "CloudbreakEvent.findCloudbreakEventsForStack",
        query = "SELECT cbe FROM CloudbreakEvent cbe "
                + "WHERE cbe.stackId= :stackId"))
@Table(name = "cloudbreakevent")
class CloudbreakEvent : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cloudbreakevent_generator")
    @SequenceGenerator(name = "cloudbreakevent_generator", sequenceName = "cloudbreakevent_id_seq", allocationSize = 1)
    var id: Long? = null

    var eventType: String? = null
    var eventTimestamp: Date? = null
    @Column(length = 1000000, columnDefinition = "TEXT")
    var eventMessage: String? = null
    var owner: String? = null
    var account: String? = null
    var cloud: String? = null
    var region: String? = null
    var availabilityZone: String? = null
    var blueprintName: String? = null
    var blueprintId: Long = 0
    var stackId: Long? = null
    var stackName: String? = null
    var clusterId: Long? = null
    var clusterName: String? = null
    var instanceGroup: String? = null
    @Enumerated(EnumType.STRING)
    var stackStatus: Status? = null
    var nodeCount: Int? = null
    @Enumerated(EnumType.STRING)
    var clusterStatus: Status? = null

    override fun toString(): String {
        val sb = StringBuilder("CloudbreakEvent{")
        sb.append("id=").append(id)
        sb.append(", eventType='").append(eventType).append('\'')
        sb.append(", eventTimestamp=").append(eventTimestamp)
        sb.append(", eventMessage='").append(eventMessage).append('\'')
        sb.append(", owner='").append(owner).append('\'')
        sb.append(", account='").append(account).append('\'')
        sb.append(", cloud='").append(cloud).append('\'')
        sb.append(", region='").append(region).append('\'')
        sb.append(", blueprintName='").append(blueprintName).append('\'')
        sb.append(", instanceGroup='").append(instanceGroup).append('\'')
        sb.append(", blueprintId=").append(blueprintId).append('\'')
        sb.append(", stackId=").append(stackId).append('\'')
        sb.append(", clusterId=").append(clusterId).append('\'')
        sb.append(", clusterName=").append(clusterName)
        sb.append('}')
        return sb.toString()
    }
}
