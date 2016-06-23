package com.sequenceiq.periscope.service.security

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.periscope.domain.Ambari
import com.sequenceiq.periscope.domain.PeriscopeUser
import com.sequenceiq.periscope.domain.SecurityConfig
import com.sequenceiq.periscope.model.AmbariStack

@Service
class ClusterSecurityService {

    @Inject
    private val cloudbreakClient: CloudbreakClient? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    fun hasAccess(user: PeriscopeUser, ambari: Ambari): Boolean {
        try {
            return hasAccess(user.id, user.account, ambari.host)
        } catch (e: Exception) {
            // if the cluster is unknown for cloudbreak
            // it should allow it to monitor
            return true
        }

    }

    @Throws(Exception::class)
    private fun hasAccess(userId: String, account: String, ambariAddress: String): Boolean {
        val ambariAddressJson = AmbariAddressJson()
        ambariAddressJson.ambariAddress = ambariAddress
        val stack = cloudbreakClient!!.stackEndpoint().getStackForAmbari(ambariAddressJson)
        if (stack.owner == userId) {
            return true
        } else if (stack.isPublicInAccount && stack.account === account) {
            return true
        }
        return false
    }

    fun tryResolve(ambari: Ambari): AmbariStack {
        try {
            val host = ambari.host
            val user = ambari.user
            val pass = ambari.pass
            val ambariAddressJson = AmbariAddressJson()
            ambariAddressJson.ambariAddress = host
            val stack = cloudbreakClient!!.stackEndpoint().getStackForAmbari(ambariAddressJson)
            val id = stack.id!!
            val securityConfig = tlsSecurityService!!.prepareSecurityConfig(id)
            if (user == null && pass == null) {
                val clusterResponse = cloudbreakClient.clusterEndpoint().get(id)
                return AmbariStack(Ambari(host, ambari.port, clusterResponse.userName, clusterResponse.password), id, securityConfig)
            } else {
                return AmbariStack(ambari, id, securityConfig)
            }
        } catch (e: Exception) {
            return AmbariStack(ambari)
        }

    }

}
