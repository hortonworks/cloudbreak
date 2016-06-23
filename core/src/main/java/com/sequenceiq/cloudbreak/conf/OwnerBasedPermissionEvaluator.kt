package com.sequenceiq.cloudbreak.conf

import java.io.Serializable
import java.lang.reflect.Field

import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils

@Component
class OwnerBasedPermissionEvaluator : PermissionEvaluator {
    private var userDetailsService: UserDetailsService? = null

    override fun hasPermission(authentication: Authentication, targetDomainObject: Any?, permission: Any): Boolean {
        if (targetDomainObject == null) {
            throw NotFoundException("Resource not found.")
        }
        val oauth = authentication as OAuth2Authentication
        if (oauth.userAuthentication == null && oauth.oAuth2Request.scope.contains(AUTO_SCALE_SCOPE)) {
            return true
        }
        try {
            val user = userDetailsService!!.getDetails(authentication.getPrincipal() as String, UserFilterField.USERNAME)
            if (getOwner(targetDomainObject) == user.userId) {
                return true
            }
            if (getAccount(targetDomainObject) == user.account) {
                if (user.roles.contains(CbUserRole.ADMIN) || isPublicInAccount(targetDomainObject)!!) {
                    return true
                }
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
    private fun getAccount(targetDomainObject: Any): String {
        var result = ""
        val accountField = ReflectionUtils.findField(targetDomainObject.javaClass, "account")
        if (accountField != null) {
            accountField.isAccessible = true
            result = accountField.get(targetDomainObject) as String
        }
        return result
    }

    @Throws(IllegalAccessException::class)
    private fun isPublicInAccount(targetDomainObject: Any): Boolean? {
        var result: Boolean? = false
        val publicInAccountField = ReflectionUtils.findField(targetDomainObject.javaClass, "publicInAccount")
        if (publicInAccountField != null) {
            publicInAccountField.isAccessible = true
            result = publicInAccountField.get(targetDomainObject) as Boolean
        }
        return result
    }

    @Throws(IllegalAccessException::class)
    private fun getOwner(targetDomainObject: Any): String {
        var result = ""
        val ownerField = ReflectionUtils.findField(targetDomainObject.javaClass, "owner")
        if (ownerField != null) {
            ownerField.isAccessible = true
            result = ownerField.get(targetDomainObject) as String
        }
        return result
    }

    companion object {

        private val AUTO_SCALE_SCOPE = "cloudbreak.autoscale"
    }
}
