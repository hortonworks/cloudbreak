package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.api.model.InstanceStatus

@Entity
@NamedQueries(@NamedQuery(
        name = "InstanceMetaData.findHostInStack",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.stack.id= :stackId "
                + "AND i.discoveryFQDN= :hostName "
                + "AND i.instanceStatus <> 'TERMINATED' "), @NamedQuery(
        name = "InstanceMetaData.findUnregisteredHostsInInstanceGroup",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.id= :instanceGroupId "
                + "AND i.instanceStatus = 'UNREGISTERED'"), @NamedQuery(
        name = "InstanceMetaData.findUnusedHostsInInstanceGroup",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.id= :instanceGroupId "
                + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED')"), @NamedQuery(
        name = "InstanceMetaData.findNotTerminatedForStack",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.stack.id= :stackId "
                + "AND i.instanceStatus <> 'TERMINATED' "), @NamedQuery(
        name = "InstanceMetaData.findAllInStack",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.stack.id= :stackId"), @NamedQuery(
        name = "InstanceMetaData.findByInstanceId",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceId= :instanceId AND i.instanceGroup.stack.id= :stackId "), @NamedQuery(
        name = "InstanceMetaData.findAliveInstancesHostNamesInInstanceGroup",
        query = "SELECT i.discoveryFQDN FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.id = :instanceGroupId "
                + "AND i.instanceStatus <> 'TERMINATED' "), @NamedQuery(
        name = "InstanceMetaData.findAliveInstancesInInstanceGroup",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.id = :instanceGroupId "
                + "AND i.instanceStatus <> 'TERMINATED' "), @NamedQuery(
        name = "InstanceMetaData.findRemovableInstances",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.stack.id= :stackId "
                + "AND i.instanceGroup.groupName= :groupName "
                + "AND i.instanceStatus in ('CREATED', 'UNREGISTERED', 'DECOMMISSIONED', 'FAILED', 'STOPPED')"), @NamedQuery(
        name = "InstanceMetaData.findNotTerminatedByPrivateAddress",
        query = "SELECT i FROM InstanceMetaData i "
                + "WHERE i.instanceGroup.stack.id= :stackId "
                + "AND i.privateIp= :privateAddress "
                + "AND i.instanceStatus <> 'TERMINATED' "))
class InstanceMetaData : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancemetadata_generator")
    @SequenceGenerator(name = "instancemetadata_generator", sequenceName = "instancemetadata_id_seq", allocationSize = 1)
    var id: Long? = null
    var privateId: Long? = null
    var privateIp: String? = null
    var publicIp: String? = null
    var sshPort: Int? = null
    var instanceId: String? = null
    var ambariServer: Boolean? = null
    var consulServer: Boolean? = null
    var discoveryFQDN: String? = null
    @Enumerated(EnumType.STRING)
    var instanceStatus: InstanceStatus? = null
    var hypervisor: String? = null
    @ManyToOne
    var instanceGroup: InstanceGroup? = null
    var startDate: Long? = null
    var terminationDate: Long? = null

    val instanceGroupName: String
        get() = instanceGroup!!.groupName

    val discoveryName: String
        get() = discoveryFQDN!!.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]

    val isCreated: Boolean
        get() = InstanceStatus.CREATED == instanceStatus

    val isFailed: Boolean
        get() = InstanceStatus.FAILED == instanceStatus

    val isDecommissioned: Boolean
        get() = InstanceStatus.DECOMMISSIONED == instanceStatus

    val isUnRegistered: Boolean
        get() = InstanceStatus.UNREGISTERED == instanceStatus

    val isTerminated: Boolean
        get() = InstanceStatus.TERMINATED == instanceStatus

    val isRegistered: Boolean
        get() = InstanceStatus.REGISTERED == instanceStatus

    val isRunning: Boolean
        get() = InstanceStatus.REGISTERED == instanceStatus || InstanceStatus.UNREGISTERED == instanceStatus

    val publicIpWrapper: String
        get() {
            if (publicIp == null) {
                return privateIp
            }
            return publicIp
        }
}
