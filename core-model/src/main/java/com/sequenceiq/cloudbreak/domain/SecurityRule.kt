package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

@Entity
@NamedQueries(@NamedQuery(
        name = "SecurityGroup.findAllBySecurityGroupId",
        query = "SELECT r FROM SecurityRule r "
                + "WHERE r.securityGroup.id= :securityGroupId"))
class SecurityRule : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityrule_generator")
    @SequenceGenerator(name = "securityrule_generator", sequenceName = "securityrule_id_seq", allocationSize = 1)
    var id: Long? = null
    @ManyToOne
    var securityGroup: SecurityGroup? = null
    var cidr: String? = null
    private var ports: String? = null
    var protocol: String? = null
    var isModifiable: Boolean = false

    val ports: Array<String>
        get() = ports!!.split(PORT_DELIMITER.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

    fun setPorts(ports: String) {
        this.ports = ports
    }

    override fun toString(): String {
        return "SecurityRule{"
        +"id=" + id
        +", securityGroup=" + securityGroup
        +", cidr='" + cidr + '\''
        +", ports='" + ports + '\''
        +", protocol='" + protocol + '\''
        +", modifiable=" + isModifiable
        +'}'
    }

    companion object {
        private val PORT_DELIMITER = ","
    }
}
