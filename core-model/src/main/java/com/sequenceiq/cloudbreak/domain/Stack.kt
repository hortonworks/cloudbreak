package com.sequenceiq.cloudbreak.domain

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.CREATE_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED
import com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.START_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.START_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.STOPPED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS

import java.util.ArrayList
import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.MapKeyColumn
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.persistence.Version

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Entity
@Table(name = "Stack", uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "Stack.findById",
        query = "SELECT c FROM Stack c "
                + "LEFT JOIN FETCH c.resources "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE c.id= :id"), @NamedQuery(
        name = "Stack.findByIdLazy",
        query = "SELECT c FROM Stack c "
                + "WHERE c.id= :id"), @NamedQuery(
        name = "Stack.findByIdWithSecurityConfig",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.securityConfig "
                + "WHERE s.id= :id"), @NamedQuery(
        name = "Stack.findByIdWithSecurityGroup",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.securityGroup sg "
                + "LEFT JOIN FETCH sg.securityRules "
                + "WHERE s.id= :id"), @NamedQuery(
        name = "Stack.findAllStackForTemplate",
        query = "SELECT distinct c FROM Stack c "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "WHERE ig.template.id= :id"), @NamedQuery(
        name = "Stack.findStackForCluster",
        query = "SELECT c FROM Stack c "
                + "LEFT JOIN FETCH c.resources "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE c.cluster.id= :id"), @NamedQuery(
        name = "Stack.findStackWithListsForCluster",
        query = "SELECT c FROM Stack c "
                + "LEFT JOIN FETCH c.resources "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE c.cluster.id= :id"), @NamedQuery(
        name = "Stack.findRequestedStacksWithCredential",
        query = "SELECT DISTINCT c FROM Stack c "
                + "LEFT JOIN FETCH c.resources "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE c.credential.id= :credentialId "
                + "AND c.status= 'REQUESTED'"), @NamedQuery(
        name = "Stack.findOneWithLists",
        query = "SELECT c FROM Stack c "
                + "LEFT JOIN FETCH c.resources "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE c.id= :id"), @NamedQuery(
        name = "Stack.findByStackResourceName",
        query = "SELECT c FROM Stack c inner join c.resources res "
                + "WHERE res.resourceName = :stackName AND res.resourceType = 'CLOUDFORMATION_STACK'"), @NamedQuery(
        name = "Stack.findForUser",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.resources "
                + "LEFT JOIN FETCH s.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE s.owner= :user "
                + "AND s.status <> 'DELETE_COMPLETED' "), @NamedQuery(
        name = "Stack.findPublicInAccountForUser",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.resources "
                + "LEFT JOIN FETCH s.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "LEFT JOIN FETCH s.cluster c "
                + "LEFT JOIN FETCH c.hostGroups "
                + "WHERE ((s.account= :account AND s.publicInAccount= true) OR s.owner= :user) "
                + "AND s.status <> 'DELETE_COMPLETED' "), @NamedQuery(
        name = "Stack.findAllInAccount",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.resources "
                + "LEFT JOIN FETCH s.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "LEFT JOIN FETCH s.cluster c "
                + "LEFT JOIN FETCH c.hostGroups "
                + "WHERE s.account= :account "
                + "AND s.status <> 'DELETE_COMPLETED' "), @NamedQuery(
        name = "Stack.findByAmbari",
        query = "SELECT s from Stack s "
                + "LEFT JOIN FETCH s.resources "
                + "LEFT JOIN FETCH s.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE s.cluster.ambariIp= :ambariIp "
                + "AND s.status <> 'DELETE_COMPLETED' "), @NamedQuery(
        name = "Stack.findOneByName",
        query = "SELECT c FROM Stack c "
                + "LEFT JOIN FETCH c.resources "
                + "LEFT JOIN FETCH c.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE c.name= :name and c.account= :account"), @NamedQuery(
        name = "Stack.findByIdInAccount",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.resources "
                + "LEFT JOIN FETCH s.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE s.id= :id and s.account= :account"), @NamedQuery(
        name = "Stack.findByNameInAccount",
        query = "SELECT s FROM Stack s "
                + "LEFT JOIN FETCH s.resources "
                + "LEFT JOIN FETCH s.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE s.name= :name and ((s.account= :account and s.publicInAccount=true) or s.owner= :owner)"), @NamedQuery(
        name = "Stack.findByNameInUser",
        query = "SELECT t FROM Stack t "
                + "LEFT JOIN FETCH t.resources "
                + "LEFT JOIN FETCH t.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE t.owner= :owner and t.name= :name"), @NamedQuery(
        name = "Stack.findByCredential",
        query = "SELECT t FROM Stack t "
                + "LEFT JOIN FETCH t.resources "
                + "LEFT JOIN FETCH t.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE t.credential.id= :credentialId "), @NamedQuery(
        name = "Stack.findAllByNetwork",
        query = "SELECT t FROM Stack t "
                + "LEFT JOIN FETCH t.resources "
                + "LEFT JOIN FETCH t.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE t.network.id= :networkId "), @NamedQuery(
        name = "Stack.findAllBySecurityGroup",
        query = "SELECT t FROM Stack t "
                + "LEFT JOIN FETCH t.resources "
                + "LEFT JOIN FETCH t.instanceGroups ig "
                + "LEFT JOIN FETCH ig.instanceMetaData "
                + "WHERE t.securityGroup.id= :securityGroupId "), @NamedQuery(
        name = "Stack.findAllAlive",
        query = "SELECT s FROM Stack s "
                + "WHERE s.status <> 'DELETE_COMPLETED' "), @NamedQuery(
        name = "Stack.findByStatuses",
        query = "SELECT s FROM Stack s "
                + "WHERE s.status IN :statuses"
), @NamedQuery(
        name = "Stack.findStacksWithoutEvents",
        query = "SELECT s.id FROM Stack s "
                + "WHERE s.id NOT IN (SELECT DISTINCT e.stackId FROM CloudbreakEvent e)"
))
class Stack : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    var id: Long? = null
    @Column(nullable = false)
    var name: String? = null
    var owner: String? = null
    var account: String? = null
    var isPublicInAccount: Boolean = false
    var region: String? = null
    var availabilityZone: String? = null
    @Column(nullable = false)
    var gatewayPort: Int? = null
    // TODO remove
    var consulServers: Int = 0
    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null
    @Column(columnDefinition = "TEXT")
    var statusReason: String? = null
    @Enumerated(EnumType.STRING)
    var status: Status? = null
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    var parameters: Map<String, String>? = null
    @OneToOne
    var credential: Credential? = null
    @Column(columnDefinition = "TEXT")
    var platformVariant: String? = null
    @Column(columnDefinition = "TEXT")
    private var cloudPlatform: String? = null
    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    var cluster: Cluster? = null
    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    var resources: Set<Resource> = HashSet()
    @Enumerated(EnumType.STRING)
    var onFailureActionAction = OnFailureAction.ROLLBACK
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    var failurePolicy: FailurePolicy? = null
    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    var securityConfig: SecurityConfig? = null
    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    var instanceGroups: Set<InstanceGroup>? = HashSet()
    @OneToMany(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    var components: Set<Component> = HashSet()
    @Version
    var version: Long? = null
    @ManyToOne
    var network: Network? = null
    @ManyToOne
    var securityGroup: SecurityGroup? = null
    @OneToOne
    var orchestrator: Orchestrator? = null
    var created: Long? = null
    var relocateDocker: Boolean? = null

    fun cloudPlatform(): String {
        return cloudPlatform
    }

    fun setCloudPlatform(cloudPlatform: String) {
        this.cloudPlatform = cloudPlatform
    }

    fun getResourcesByType(resourceType: ResourceType): List<Resource> {
        val resourceList = ArrayList<Resource>()
        for (resource in resources) {
            if (resourceType == resource.resourceType) {
                resourceList.add(resource)
            }
        }
        return resourceList
    }

    fun getResourceByType(resourceType: ResourceType): Resource? {
        for (resource in resources) {
            if (resourceType == resource.resourceType) {
                return resource
            }
        }
        return null
    }

    fun getInstanceGroupByInstanceGroupId(groupId: Long?): InstanceGroup? {
        for (instanceGroup in instanceGroups!!) {
            if (groupId == instanceGroup.id) {
                return instanceGroup
            }
        }
        return null
    }

    fun getInstanceGroupByInstanceGroupName(group: String): InstanceGroup? {
        for (instanceGroup in instanceGroups!!) {
            if (group == instanceGroup.groupName) {
                return instanceGroup
            }
        }
        return null
    }

    val fullNodeCount: Int?
        get() {
            var nodeCount = 0
            for (instanceGroup in instanceGroups!!) {
                nodeCount += instanceGroup.nodeCount!!
            }
            return nodeCount
        }

    val fullNodeCountWithoutDecommissionedNodes: Int?
        get() {
            var nodeCount = 0
            for (instanceGroup in instanceGroups!!) {
                for (instanceMetaData in instanceGroup.instanceMetaData) {
                    if (instanceMetaData.instanceStatus != InstanceStatus.DECOMMISSIONED) {
                        nodeCount++
                    }
                }
            }
            return nodeCount
        }

    val fullNodeCountWithoutDecommissionedAndUnRegisteredNodes: Int?
        get() {
            var nodeCount = 0
            for (instanceGroup in instanceGroups!!) {
                for (instanceMetaData in instanceGroup.instanceMetaData) {
                    if (instanceMetaData.instanceStatus == InstanceStatus.REGISTERED) {
                        nodeCount++
                    }
                }
            }
            return nodeCount
        }

    val runningInstanceMetaData: Set<InstanceMetaData>
        get() {
            val instanceMetadata = HashSet<InstanceMetaData>()
            for (instanceGroup in instanceGroups!!) {
                instanceMetadata.addAll(instanceGroup.instanceMetaData)
            }
            return instanceMetadata
        }

    val instanceMetaDataAsList: List<InstanceMetaData>
        get() = ArrayList(runningInstanceMetaData)

    val instanceGroupsAsList: List<InstanceGroup>
        get() = ArrayList(instanceGroups)

    val isStackInDeletionPhase: Boolean
        get() = status == DELETE_COMPLETED || status == DELETE_IN_PROGRESS

    val isStopFailed: Boolean
        get() = STOP_FAILED == status

    val isStackInStopPhase: Boolean
        get() = STOP_IN_PROGRESS == status || STOPPED == status

    val isStartFailed: Boolean
        get() = START_FAILED == status

    val gatewayInstanceGroup: InstanceGroup?
        get() {
            for (instanceGroup in instanceGroups!!) {
                if (InstanceGroupType.GATEWAY == instanceGroup.instanceGroupType) {
                    return instanceGroup
                }
            }
            return null
        }

    val gateWayNodeCount: Int
        get() = gatewayInstanceGroup!!.nodeCount!!

    val ambariIp: String?
        get() = if (cluster == null) null else cluster!!.ambariIp

    val isAvailable: Boolean
        get() = AVAILABLE == status

    val isStopRequested: Boolean
        get() = STOP_REQUESTED == status

    val isStopped: Boolean
        get() = STOPPED == status

    val isDeleteCompleted: Boolean
        get() = DELETE_COMPLETED == status

    val isDeleteInProgress: Boolean
        get() = DELETE_IN_PROGRESS == status

    val isStartInProgress: Boolean
        get() = START_IN_PROGRESS == status || START_REQUESTED == status

    val isRequested: Boolean
        get() = REQUESTED == status || CREATE_IN_PROGRESS == status

    val isStackReadyForStop: Boolean
        get() = AVAILABLE == status || STOP_REQUESTED == status

    val isModificationInProgress: Boolean
        get() = CREATE_IN_PROGRESS == status
                || UPDATE_IN_PROGRESS == status
                || STOP_IN_PROGRESS == status
                || START_IN_PROGRESS == status
                || DELETE_IN_PROGRESS == status

    fun infrastructureIsEphemeral(): Boolean {
        var ephemeral = false
        if ("AWS" == cloudPlatform()) {
            for (instanceGroup in instanceGroups!!) {
                if ("ephemeral" == instanceGroup.template.volumeType) {
                    ephemeral = true
                    break
                }
            }
        }
        return ephemeral
    }

    val isInstanceGroupsSpecified: Boolean
        get() = instanceGroups != null && !instanceGroups!!.isEmpty()

}
