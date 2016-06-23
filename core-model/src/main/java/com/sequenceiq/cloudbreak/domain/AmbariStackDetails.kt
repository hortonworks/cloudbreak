package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

@Entity
class AmbariStackDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "ambaristackdetails_generator")
    @SequenceGenerator(name = "ambaristackdetails_generator", sequenceName = "ambaristackdetails_id_seq", allocationSize = 1)
    var id: Long? = null
    var stack: String? = null
    var version: String? = null
    var os: String? = null
    var stackRepoId: String? = null
    var stackBaseURL: String? = null
    var utilsRepoId: String? = null
    var utilsBaseURL: String? = null
    var isVerify = true

    override fun toString(): String {
        val sb = StringBuilder("AmbariStackDetails{")
        sb.append("id=").append(id)
        sb.append(", stack='").append(stack).append('\'')
        sb.append(", version='").append(version).append('\'')
        sb.append(", os='").append(os).append('\'')
        sb.append(", stackRepoId='").append(stackRepoId).append('\'')
        sb.append(", utilsRepoId='").append(utilsRepoId).append('\'')
        sb.append(", stackBaseURL='").append(stackBaseURL).append('\'')
        sb.append(", utilsBaseURL='").append(utilsBaseURL).append('\'')
        sb.append(", verify=").append(isVerify)
        sb.append('}')
        return sb.toString()
    }
}
