package com.sequenceiq.periscope.service.security

import java.io.IOException
import java.io.UnsupportedEncodingException

import javax.annotation.PostConstruct
import javax.ws.rs.client.Client
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType

import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.crypto.codec.Base64
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.client.AccessToken
import com.sequenceiq.cloudbreak.client.IdentityClient
import com.sequenceiq.periscope.domain.PeriscopeUser

@Service
class RemoteUserDetailsService : UserDetailsService {

    @Autowired
    private val restClient: Client? = null

    @Autowired
    private val identityClient: IdentityClient? = null

    @Autowired
    @Qualifier("identityServerUrl")
    private val identityServerUrl: String? = null

    @Value("${periscope.client.secret}")
    private val clientSecret: String? = null

    private var identityWebTarget: WebTarget? = null

    @PostConstruct
    fun init() {
        identityWebTarget = restClient!!.target(identityServerUrl).path("Users")
    }

    @Cacheable("userCache")
    @SuppressWarnings("unchecked")
    override fun getDetails(filterValue: String, filterField: UserFilterField): PeriscopeUser {
        val target: WebTarget
        when (filterField) {
            UserFilterField.USERNAME -> target = identityWebTarget!!.queryParam("filter", "userName eq \"" + filterValue + "\"")
            UserFilterField.USER_ID -> target = identityWebTarget!!.path(filterValue)
            else -> throw UserDetailsUnavailableException("User details cannot be retrieved.")
        }
        val accessToken = identityClient!!.getToken(clientSecret)
        val scimResponse = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + accessToken.token).get<String>(String::class.java)
        val mapper = ObjectMapper()
        try {
            val root = mapper.readTree(scimResponse)
            var userNode = root
            if (UserFilterField.USERNAME == filterField) {
                userNode = root.get("resources").get(0)
            }
            var account: String? = null
            val iterator = userNode.get("groups").getElements()
            while (iterator.hasNext()) {
                val node = iterator.next()
                val group = node.get("display").asText()
                if (group.startsWith("sequenceiq.account")) {
                    val parts = group.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (account != null && account != parts[ACCOUNT_PART]) {
                        throw IllegalStateException("A user can belong to only one account.")
                    }
                    account = parts[ACCOUNT_PART]
                }
            }
            val userId = userNode.get("id").asText()
            val email = userNode.get("userName").asText()
            return PeriscopeUser(userId, email, account)
        } catch (e: IOException) {
            throw UserDetailsUnavailableException("User details cannot be retrieved from identity server.", e)
        }

    }

    private fun getAuthorizationHeader(clientId: String, clientSecret: String): String {
        val creds = String.format("%s:%s", clientId, clientSecret)
        try {
            return "Basic " + String(Base64.encode(creds.toByteArray(charset("UTF-8"))))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("Could not convert String")
        }

    }

    companion object {

        private val ACCOUNT_PART = 2
    }
}
