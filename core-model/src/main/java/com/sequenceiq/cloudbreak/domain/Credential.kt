package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.domain.json.JsonToString

@Entity
@Table(uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "Credential.findByTopology",
        query = "SELECT c FROM Credential c "
                + "WHERE c.topology.id= :topologyId"), @NamedQuery(
        name = "Credential.findForUser",
        query = "SELECT c FROM Credential c "
                + "WHERE c.owner= :user"
                + " AND c.archived IS FALSE"), @NamedQuery(
        name = "Credential.findPublicInAccountForUser",
        query = "SELECT c FROM Credential c "
                + "WHERE (c.account= :account AND c.publicInAccount= true) "
                + "OR c.owner= :user"
                + " AND c.archived IS FALSE"), @NamedQuery(
        name = "Credential.findOneByName",
        query = "SELECT b FROM Credential b "
                + "WHERE b.name= :name and b.account= :account"
                + " AND b.archived IS FALSE"), @NamedQuery(
        name = "Credential.findAllInAccount",
        query = "SELECT c FROM Credential c "
                + "WHERE c.account= :account"
                + " AND c.archived IS FALSE"), @NamedQuery(
        name = "Credential.findByIdInAccount",
        query = "SELECT c FROM Credential c "
                + "WHERE c.id= :id and c.account= :account"
                + " AND c.archived IS FALSE"), @NamedQuery(
        name = "Credential.findByNameInAccount",
        query = "SELECT c FROM Credential c "
                + "WHERE c.name= :name and ((c.publicInAccount=true and c.account= :account) or c.owner= :owner)"
                + " AND c.archived IS FALSE"), @NamedQuery(
        name = "Credential.findByNameInUser",
        query = "SELECT c FROM Credential c "
                + "WHERE c.owner= :owner and c.name= :name"
                + " AND c.archived IS FALSE"))
class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_id_seq", allocationSize = 1)
    var id: Long? = null

    @Column(nullable = false)
    var name: String? = null

    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null

    var owner: String? = null
    var account: String? = null

    var loginUserName: String? = null

    var isPublicInAccount: Boolean = false

    @Column(columnDefinition = "TEXT")
    var publicKey: String? = null

    @Column(columnDefinition = "boolean default false")
    var isArchived: Boolean = false

    @Column(nullable = false)
    private var cloudPlatform: String? = null

    @Convert(converter = JsonToString::class)
    @Column(columnDefinition = "TEXT")
    var attributes: Json? = null

    @ManyToOne
    var topology: Topology? = null

    fun passwordAuthenticationRequired(): Boolean {
        return publicKey!!.startsWith("Basic:")
    }

    val loginPassword: String
        get() = publicKey!!.replace("Basic:".toRegex(), "").trim({ it <= ' ' })

    fun cloudPlatform(): String {
        return cloudPlatform
    }

    fun setCloudPlatform(cloudPlatform: String) {
        this.cloudPlatform = cloudPlatform
    }
}
