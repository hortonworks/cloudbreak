package com.sequenceiq.cloudbreak.service.user

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.client.Client
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.codec.Base64
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import com.fasterxml.jackson.databind.JsonNode
import com.google.api.client.repackaged.com.google.common.base.Strings
import com.sequenceiq.cloudbreak.client.AccessToken
import com.sequenceiq.cloudbreak.client.IdentityClient
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.BlueprintRepository
import com.sequenceiq.cloudbreak.repository.CredentialRepository
import com.sequenceiq.cloudbreak.repository.NetworkRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.TemplateRepository
import com.sequenceiq.cloudbreak.util.JsonUtil

@Service
class RemoteUserDetailsService : UserDetailsService {

    @Value("${cb.client.secret}")
    private val clientSecret: String? = null

    @Inject
    @Named("identityServerUrl")
    private val identityServerUrl: String? = null

    @Inject
    private val restClient: Client? = null

    @Inject
    private val identityClient: IdentityClient? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val credentialRepository: CredentialRepository? = null

    @Inject
    private val templateRepository: TemplateRepository? = null

    @Inject
    private val blueprintRepository: BlueprintRepository? = null

    @Inject
    private val networkRepository: NetworkRepository? = null

    private var identityWebTarget: WebTarget? = null

    @PostConstruct
    fun init() {
        identityWebTarget = restClient!!.target(identityServerUrl).path("Users")
    }

    @Cacheable(value = "userCache", key = "#filterValue")
    override fun getDetails(filterValue: String, filterField: UserFilterField): CbUser {
        val target: WebTarget
        when (filterField) {
            UserFilterField.USERNAME -> target = identityWebTarget!!.queryParam("filter", "userName eq \"" + filterValue + "\"")
            UserFilterField.USERID -> target = identityWebTarget!!.path(filterValue)
            else -> throw UserDetailsUnavailableException("User details cannot be retrieved.")
        }
        val accessToken = identityClient!!.getToken(clientSecret)
        val scimResponse = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + accessToken.token).get<String>(String::class.java)
        try {
            val root = JsonUtil.readTree(scimResponse)
            val roles = ArrayList<CbUserRole>()
            var account: String? = null
            var userNode = root
            if (UserFilterField.USERNAME == filterField) {
                userNode = root.get("resources").get(0)
            }
            val iterator = userNode.get("groups").iterator()
            while (iterator.hasNext()) {
                val node = iterator.next()
                val group = node.get("display").asText()
                if (group.startsWith("sequenceiq.account")) {
                    val parts = group.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (account != null && account != parts[ACCOUNT_PART]) {
                        throw IllegalStateException("A user can belong to only one account.")
                    }
                    account = parts[ACCOUNT_PART]
                } else if (group.startsWith("sequenceiq.cloudbreak")) {
                    val parts = group.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    roles.add(CbUserRole.fromString(parts[ROLE_PART]))
                }
            }
            val userId = userNode.get("id").asText()
            val email = userNode.get("userName").asText()
            val givenName = getGivenName(userNode)
            val familyName = getFamilyName(userNode)
            val dateOfCreation = userNode.get("meta").get("created").asText()
            val created = parseUserCreated(dateOfCreation)
            return CbUser(userId, email, account, roles, givenName, familyName, created)
        } catch (e: IOException) {
            throw UserDetailsUnavailableException("User details cannot be retrieved from identity server.", e)
        }

    }

    private fun getGivenName(userNode: JsonNode): String {
        if (userNode.get("name") != null) {
            if (userNode.get("name").get("givenName") != null) {
                return userNode.get("name").get("givenName").asText()
            }
        }
        return ""
    }

    private fun getFamilyName(userNode: JsonNode): String {
        if (userNode.get("name") != null) {
            if (userNode.get("name").get("familyName") != null) {
                return userNode.get("name").get("familyName").asText()
            }
        }
        return ""
    }

    @CacheEvict(value = "userCache", key = "#filterValue")
    override fun evictUserDetails(updatedUserId: String, filterValue: String) {
        LOGGER.info("Remove userid: {} / username: {} from user cache", updatedUserId, filterValue)
    }

    @Transactional(readOnly = true)
    override fun hasResources(admin: CbUser, userId: String): Boolean {
        val user = getDetails(userId, UserFilterField.USERID)
        LOGGER.info("{} / {} checks resources of {}", admin.userId, admin.username, userId)
        var errorMessage: String? = null
        if (!admin.roles.contains(CbUserRole.ADMIN)) {
            errorMessage = "Forbidden: user (%s) is not authorized for this operation on %s"
        }
        if (admin.account != user.account) {
            errorMessage = "Forbidden: admin (%s) and user (%s) are not under the same account."
        }
        if (!Strings.isNullOrEmpty(errorMessage)) {
            throw AccessDeniedException(String.format(errorMessage, admin.username, user.username))
        }
        val templates = templateRepository!!.findForUser(user.userId)
        val credentials = credentialRepository!!.findForUser(user.userId)
        val blueprints = blueprintRepository!!.findForUser(user.userId)
        val networks = networkRepository!!.findForUser(user.userId)
        val stacks = stackRepository!!.findForUser(user.userId)
        return !(stacks.isEmpty() && templates.isEmpty() && credentials.isEmpty()
                && blueprints.isEmpty() && networks.isEmpty())
    }


    private fun getAuthorizationHeader(clientId: String, clientSecret: String): String {
        val creds = String.format("%s:%s", clientId, clientSecret)
        try {
            return "Basic " + String(Base64.encode(creds.toByteArray(charset("UTF-8"))))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Could not convert String")
        }

    }

    private fun parseUserCreated(dateOfCreation: String): Date {
        try {
            val uaaDateFormat = SimpleDateFormat(UAA_DATE_PATTERN)
            return uaaDateFormat.parse(dateOfCreation)
        } catch (e: ParseException) {
            throw UserDetailsUnavailableException("User details cannot be retrieved, becuase creation date of user cannot be parsed.", e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(RemoteUserDetailsService::class.java)
        private val ACCOUNT_PART = 2
        private val ROLE_PART = 2
        private val UAA_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    }
}
