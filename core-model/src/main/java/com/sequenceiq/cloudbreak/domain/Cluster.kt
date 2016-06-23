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

import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Column
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
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import com.sequenceiq.cloudbreak.api.model.ConfigStrategy
import com.sequenceiq.cloudbreak.api.model.Status

@Entity
@Table(name = "Cluster", uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "Cluster.findAllClustersByBlueprint",
        query = "SELECT c FROM Cluster c "
                + "WHERE c.blueprint.id= :id"), @NamedQuery(
        name = "Cluster.findAllClustersBySssdConfig",
        query = "SELECT c FROM Cluster c "
                + "WHERE c.sssdConfig.id= :id"), @NamedQuery(
        name = "Cluster.findOneWithLists",
        query = "SELECT c FROM Cluster c "
                + "LEFT JOIN FETCH c.hostGroups "
                + "WHERE c.id= :id"), @NamedQuery(
        name = "Cluster.findByStatuses",
        query = "SELECT c FROM Cluster c "
                + "WHERE c.status IN :statuses"
), @NamedQuery(
        name = "Cluster.findByNameInAccount",
        query = "SELECT c FROM Cluster c "
                + "WHERE c.name= :name and c.account= :account"), @NamedQuery(
        name = "Cluster.findAllClustersForConstraintTemplate",
        query = "SELECT c FROM Cluster c inner join c.hostGroups hg "
                + "WHERE hg.constraint.constraintTemplate.id = :id"))
class Cluster : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "cluster_id_seq", allocationSize = 1)
    var id: Long? = null

    @OneToOne
    var stack: Stack? = null

    @ManyToOne
    var blueprint: Blueprint? = null

    @Column(nullable = false)
    var name: String? = null

    var owner: String? = null
    var account: String? = null
    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null

    @Enumerated(EnumType.STRING)
    var status: Status? = null

    var creationStarted: Long? = null

    var creationFinished: Long? = null

    var upSince: Long? = null

    @Column(length = 1000000, columnDefinition = "TEXT")
    var statusReason: String? = null

    var ambariIp: String? = null

    var userName: String? = null
    var password: String? = null

    var secure: Boolean? = null
    var kerberosMasterKey: String? = null
    var kerberosAdmin: String? = null
    var kerberosPassword: String? = null

    var isLdapRequired: Boolean? = null
        get() = if (isLdapRequired == null) false else isLdapRequired
    var enableShipyard: Boolean? = null

    @ManyToOne
    var sssdConfig: SssdConfig? = null

    var emailNeeded: Boolean? = null

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
    var hostGroups: Set<HostGroup> = HashSet()

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
    var containers: Set<Container> = HashSet()

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    var ambariStackDetails: AmbariStackDetails? = null

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    var rdsConfig: RDSConfig? = null

    @ManyToOne
    var fileSystem: FileSystem? = null

    @Enumerated(EnumType.STRING)
    var configStrategy: ConfigStrategy? = null

    val isCreateFailed: Boolean
        get() = status == Status.CREATE_FAILED

    val isSecure: Boolean
        get() = if (secure == null) false else secure

    val isClusterReadyForStop: Boolean
        get() = AVAILABLE == status || STOPPED == status

    val isAvailable: Boolean
        get() = AVAILABLE == status

    val isStopped: Boolean
        get() = STOPPED == status

    val isStopFailed: Boolean
        get() = STOP_FAILED == status

    val isStartFailed: Boolean
        get() = START_FAILED == status

    val isStartRequested: Boolean
        get() = START_REQUESTED == status

    val isStopInProgress: Boolean
        get() = STOP_IN_PROGRESS == status || STOP_REQUESTED == status

    val isRequested: Boolean
        get() = REQUESTED == status

    val isDeleteInProgress: Boolean
        get() = DELETE_IN_PROGRESS == status

    val isDeleteCompleted: Boolean
        get() = DELETE_COMPLETED == status

    val isClusterReadyForStart: Boolean
        get() = STOPPED == status || START_REQUESTED == status

    val isModificationInProgress: Boolean
        get() = CREATE_IN_PROGRESS == status
                || UPDATE_IN_PROGRESS == status
                || STOP_IN_PROGRESS == status
                || START_IN_PROGRESS == status
                || DELETE_IN_PROGRESS == status
}
