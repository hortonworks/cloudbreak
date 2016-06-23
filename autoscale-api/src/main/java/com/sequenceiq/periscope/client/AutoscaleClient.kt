package com.sequenceiq.periscope.client

import java.util.Collections
import java.util.concurrent.TimeUnit

import javax.ws.rs.client.Client
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.Form
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap

import org.glassfish.jersey.client.proxy.WebResourceFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.client.AccessToken
import com.sequenceiq.cloudbreak.client.ConfigKey
import com.sequenceiq.cloudbreak.client.IdentityClient
import com.sequenceiq.cloudbreak.client.RestClientUtil
import com.sequenceiq.periscope.api.AutoscaleApi
import com.sequenceiq.periscope.api.endpoint.AlertEndpoint
import com.sequenceiq.periscope.api.endpoint.ClusterEndpoint
import com.sequenceiq.periscope.api.endpoint.ConfigurationEndpoint
import com.sequenceiq.periscope.api.endpoint.HistoryEndpoint
import com.sequenceiq.periscope.api.endpoint.PolicyEndpoint

import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap

class AutoscaleClient {

    private val tokenCache: ExpiringMap<String, String>

    private val client: Client
    private val identityClient: IdentityClient
    private val autoscaleAddress: String

    private val user: String
    private val password: String

    private val secret: String?

    private var t: WebTarget? = null

    private var alertEndpoint: AlertEndpoint? = null
    private var clusterEndpoint: ClusterEndpoint? = null
    private var configurationEndpoint: ConfigurationEndpoint? = null
    private var historyEndpoint: HistoryEndpoint? = null
    private var policyEndpoint: PolicyEndpoint? = null

    private constructor(autoscaleAddress: String, identityServerAddress: String, user: String, password: String, clientId: String, configKey: ConfigKey) {
        this.client = RestClientUtil.get(configKey)
        this.autoscaleAddress = autoscaleAddress
        this.identityClient = IdentityClient(identityServerAddress, clientId, configKey)
        this.user = user
        this.password = password
        this.tokenCache = configTokenCache()
        refresh()
        LOGGER.info("AutoscaleClient has been created with user / pass. autoscale: {}, identity: {}, clientId: {}, configKey: {}", autoscaleAddress,
                identityServerAddress, clientId, configKey)
    }

    private constructor(autoscaleAddress: String, identityServerAddress: String, secret: String, clientId: String, configKey: ConfigKey) {
        this.client = RestClientUtil.get(configKey)
        this.autoscaleAddress = autoscaleAddress
        this.identityClient = IdentityClient(identityServerAddress, clientId, configKey)
        this.secret = secret
        this.tokenCache = configTokenCache()
        refresh()
        LOGGER.info("AutoscaleClient has been created with a secret. autoscale: {}, identity: {}, clientId: {}, configKey: {}", autoscaleAddress,
                identityServerAddress, clientId, configKey)
    }

    private fun configTokenCache(): ExpiringMap<String, String> {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build<String, String>()
    }

    @Synchronized private fun refresh() {
        var token: String? = tokenCache[TOKEN_KEY]
        if (token == null) {
            val accessToken: AccessToken
            if (secret != null) {
                accessToken = identityClient.getToken(secret)
            } else {
                accessToken = identityClient.getToken(user, password)
            }
            token = accessToken.token
            val exp = (accessToken.expiresIn * TOKEN_EXPIRATION_FACTOR).toInt()
            LOGGER.info("Token has been renewed and expires in {} seconds", exp)
            tokenCache.put(TOKEN_KEY, accessToken.token, ExpirationPolicy.CREATED, exp.toLong(), TimeUnit.SECONDS)
            renewEndpoints(token)
        }
    }

    private fun renewEndpoints(token: String) {
        val headers = MultivaluedHashMap<String, Any>()
        headers.add("Authorization", "Bearer " + token)
        this.t = client.target(autoscaleAddress).path(AutoscaleApi.API_ROOT_CONTEXT)
        this.alertEndpoint = newResource<AlertEndpoint>(AlertEndpoint::class.java, headers)
        this.clusterEndpoint = newResource<ClusterEndpoint>(ClusterEndpoint::class.java, headers)
        this.configurationEndpoint = newResource<ConfigurationEndpoint>(ConfigurationEndpoint::class.java, headers)
        this.historyEndpoint = newResource<HistoryEndpoint>(HistoryEndpoint::class.java, headers)
        this.policyEndpoint = newResource<PolicyEndpoint>(PolicyEndpoint::class.java, headers)
        LOGGER.info("Endpoints have been renewed for AutoscaleClient")
    }

    private fun <C> newResource(resourceInterface: Class<C>, headers: MultivaluedMap<String, Any>): C {
        return WebResourceFactory.newResource(resourceInterface, t, false, headers, emptyList<Cookie>(), EMPTY_FORM)
    }

    fun alertEndpoint(): AlertEndpoint {
        refresh()
        return alertEndpoint
    }

    fun clusterEndpoint(): ClusterEndpoint {
        refresh()
        return clusterEndpoint
    }

    fun configurationEndpoint(): ConfigurationEndpoint {
        refresh()
        return configurationEndpoint
    }

    fun historyEndpoint(): HistoryEndpoint {
        refresh()
        return historyEndpoint
    }

    fun policyEndpoint(): PolicyEndpoint {
        refresh()
        return policyEndpoint
    }

    class AutoscaleClientBuilder(private val autoscaleAddress: String, private val identityServerAddress: String, private val clientId: String) {

        private var user: String? = null
        private var password: String? = null

        private var secret: String? = null

        private var debug: Boolean = false

        private var secure = true

        fun withCredential(user: String, password: String): AutoscaleClientBuilder {
            this.user = user
            this.password = password
            return this
        }

        fun withSecret(secret: String): AutoscaleClientBuilder {
            this.secret = secret
            return this
        }

        fun withDebug(debug: Boolean): AutoscaleClientBuilder {
            this.debug = debug
            return this
        }

        fun withCertificateValidation(secure: Boolean): AutoscaleClientBuilder {
            this.secure = secure
            return this
        }


        fun build(): AutoscaleClient {
            val configKey = ConfigKey(secure, debug)
            if (secret != null) {
                return AutoscaleClient(autoscaleAddress, identityServerAddress, secret, clientId, configKey)
            } else {
                return AutoscaleClient(autoscaleAddress, identityServerAddress, user, password, clientId, configKey)
            }
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AutoscaleClient::class.java)

        private val EMPTY_FORM = Form()
        private val TOKEN_KEY = "TOKEN"
        private val TOKEN_EXPIRATION_FACTOR = 0.9
    }

}
