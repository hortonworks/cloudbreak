package com.sequenceiq.periscope.service.security

import java.io.Serializable
import java.lang.reflect.Field

import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.PeriscopeUser
import com.sequenceiq.periscope.service.NotFoundException

@Component
class OwnerBasedPermissionEvaluator : PermissionEvaluator {

    private var userDetailsService: UserDetailsService? = null

    override fun hasPermission(authentication: Authentication, targetDomainObject: Any?, permission: Any): Boolean {
        if (targetDomainObject == null) {
            throw NotFoundException("Resource not found.")
        }
        try {
            val user = userDetailsService!!.getDetails(authentication.principal as String, UserFilterField.USERNAME)
            if (getUserId(targetDomainObject) == user.id) {
                return true
            }
        } catch (e: IllegalAccessException) {
            return false
        }

        return false
    }

    override fun hasPermission(authentication: Authentication, targetId: Serializable, targetType: String, permission: Any): Boolean {
        return false
    }

    fun setUserDetailsService(userDetailsService: UserDetailsService) {
        this.userDetailsService = userDetailsService
    }

    @Throws(IllegalAccessException::class)
    private fun getUserId(targetDomainObject: Any): String {
        val clusterField = ReflectionUtils.findField(targetDomainObject.javaClass, "cluster")
        if (clusterField != null) {
            clusterField.isAccessible = true
            val cluster = clusterField.get(targetDomainObject) as Cluster
            return getUserId(cluster)
        } else {
            val userIdField = ReflectionUtils.findField(targetDomainObject.javaClass, "userId")
            if (userIdField != null) {
                userIdField.isAccessible = true
                return userIdField.get(targetDomainObject) as String
            }
            return getUserIdFromCluster(targetDomainObject)
        }
    }

    @Throws(IllegalAccessException::class)
    private fun getUserIdFromCluster(targetDomainObject: Any): String {
        val owner = ReflectionUtils.findField(targetDomainObject.javaClass, "user")
        owner.isAccessible = true
        val user = owner.get(targetDomainObject) as PeriscopeUser
        return user.id
    }
}
