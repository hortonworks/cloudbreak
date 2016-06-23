package com.sequenceiq.cloudbreak.client

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

import com.sequenceiq.cloudbreak.api.CoreApi
import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.ConstraintTemplateEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.EventEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.SubscriptionEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.TopologyEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.UsageEndpoint
import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint

import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap

class CloudbreakClient {

    private val tokenCache: ExpiringMap<String, String>

    private val client: Client
    private val identityClient: IdentityClient
    private val cloudbreakAddress: String


    private val user: String
    private val password: String

    private val secret: String?

    private var t: WebTarget? = null

    private var credentialEndpoint: CredentialEndpoint? = null
    private var templateEndpoint: TemplateEndpoint? = null
    private var topologyEndpoint: TopologyEndpoint? = null
    private var usageEndpoint: UsageEndpoint? = null
    private var userEndpoint: UserEndpoint? = null
    private var eventEndpoint: EventEndpoint? = null
    private var securityGroupEndpoint: SecurityGroupEndpoint? = null
    private var stackEndpoint: StackEndpoint? = null
    private var subscriptionEndpoint: SubscriptionEndpoint? = null
    private var networkEndpoint: NetworkEndpoint? = null
    private var recipeEndpoint: RecipeEndpoint? = null
    private var sssdConfigEndpoint: SssdConfigEndpoint? = null
    private var accountPreferencesEndpoint: AccountPreferencesEndpoint? = null
    private var blueprintEndpoint: BlueprintEndpoint? = null
    private var clusterEndpoint: ClusterEndpoint? = null
    private var connectorEndpoint: ConnectorEndpoint? = null
    private var constraintTemplateEndpoint: ConstraintTemplateEndpoint? = null

    private constructor(cloudbreakAddress: String, identityServerAddress: String, user: String, password: String, clientId: String, configKey: ConfigKey) {
        this.client = RestClientUtil[configKey]
        this.cloudbreakAddress = cloudbreakAddress
        this.identityClient = IdentityClient(identityServerAddress, clientId, configKey)
        this.user = user
        this.password = password
        this.tokenCache = configTokenCache()
        refresh()
        LOGGER.info("CloudbreakClient has been created with user / pass. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey)
    }

    private constructor(cloudbreakAddress: String, identityServerAddress: String, secret: String, clientId: String, configKey: ConfigKey) {
        this.client = RestClientUtil[configKey]
        this.cloudbreakAddress = cloudbreakAddress
        this.identityClient = IdentityClient(identityServerAddress, clientId, configKey)
        this.secret = secret
        this.tokenCache = configTokenCache()
        refresh()
        LOGGER.info("CloudbreakClient has been created with a secret. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
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
        this.t = client.target(cloudbreakAddress).path(CoreApi.API_ROOT_CONTEXT)
        this.credentialEndpoint = newResource<CredentialEndpoint>(CredentialEndpoint::class.java, headers)
        this.templateEndpoint = newResource<TemplateEndpoint>(TemplateEndpoint::class.java, headers)
        this.topologyEndpoint = newResource<TopologyEndpoint>(TopologyEndpoint::class.java, headers)
        this.usageEndpoint = newResource<UsageEndpoint>(UsageEndpoint::class.java, headers)
        this.eventEndpoint = newResource<EventEndpoint>(EventEndpoint::class.java, headers)
        this.securityGroupEndpoint = newResource<SecurityGroupEndpoint>(SecurityGroupEndpoint::class.java, headers)
        this.stackEndpoint = newResource<StackEndpoint>(StackEndpoint::class.java, headers)
        this.subscriptionEndpoint = newResource<SubscriptionEndpoint>(SubscriptionEndpoint::class.java, headers)
        this.networkEndpoint = newResource<NetworkEndpoint>(NetworkEndpoint::class.java, headers)
        this.recipeEndpoint = newResource<RecipeEndpoint>(RecipeEndpoint::class.java, headers)
        this.sssdConfigEndpoint = newResource<SssdConfigEndpoint>(SssdConfigEndpoint::class.java, headers)
        this.accountPreferencesEndpoint = newResource<AccountPreferencesEndpoint>(AccountPreferencesEndpoint::class.java, headers)
        this.blueprintEndpoint = newResource<BlueprintEndpoint>(BlueprintEndpoint::class.java, headers)
        this.clusterEndpoint = newResource<ClusterEndpoint>(ClusterEndpoint::class.java, headers)
        this.connectorEndpoint = newResource<ConnectorEndpoint>(ConnectorEndpoint::class.java, headers)
        this.userEndpoint = newResource<UserEndpoint>(UserEndpoint::class.java, headers)
        this.constraintTemplateEndpoint = newResource<ConstraintTemplateEndpoint>(ConstraintTemplateEndpoint::class.java, headers)
        LOGGER.info("Endpoints have been renewed for CloudbreakClient")
    }

    private fun <C> newResource(resourceInterface: Class<C>, headers: MultivaluedMap<String, Any>): C {
        return WebResourceFactory.newResource(resourceInterface, t, false, headers, emptyList<Cookie>(), EMPTY_FORM)
    }

    fun credentialEndpoint(): CredentialEndpoint {
        refresh()
        return credentialEndpoint
    }

    fun templateEndpoint(): TemplateEndpoint {
        refresh()
        return templateEndpoint
    }

    fun topologyEndpoint(): TopologyEndpoint {
        refresh()
        return topologyEndpoint
    }

    fun usageEndpoint(): UsageEndpoint {
        refresh()
        return usageEndpoint
    }

    fun userEndpoint(): UserEndpoint {
        refresh()
        return userEndpoint
    }

    fun eventEndpoint(): EventEndpoint {
        refresh()
        return eventEndpoint
    }

    fun securityGroupEndpoint(): SecurityGroupEndpoint {
        refresh()
        return securityGroupEndpoint
    }

    fun stackEndpoint(): StackEndpoint {
        refresh()
        return stackEndpoint
    }

    fun subscriptionEndpoint(): SubscriptionEndpoint {
        refresh()
        return subscriptionEndpoint
    }

    fun networkEndpoint(): NetworkEndpoint {
        refresh()
        return networkEndpoint
    }

    fun recipeEndpoint(): RecipeEndpoint {
        refresh()
        return recipeEndpoint
    }

    fun sssdConfigEndpoint(): SssdConfigEndpoint {
        refresh()
        return sssdConfigEndpoint
    }

    fun accountPreferencesEndpoint(): AccountPreferencesEndpoint {
        refresh()
        return accountPreferencesEndpoint
    }

    fun blueprintEndpoint(): BlueprintEndpoint {
        refresh()
        return blueprintEndpoint
    }

    fun clusterEndpoint(): ClusterEndpoint {
        refresh()
        return clusterEndpoint
    }

    fun connectorEndpoint(): ConnectorEndpoint {
        refresh()
        return connectorEndpoint
    }

    fun constraintTemplateEndpoint(): ConstraintTemplateEndpoint {
        return constraintTemplateEndpoint
    }

    class CloudbreakClientBuilder(private val cloudbreakAddress: String, private val identityServerAddress: String, private val clientId: String) {

        private var user: String? = null
        private var password: String? = null

        private var secret: String? = null

        private var debug: Boolean = false

        private var secure = true

        fun withCredential(user: String, password: String): CloudbreakClientBuilder {
            this.user = user
            this.password = password
            return this
        }

        fun withSecret(secret: String): CloudbreakClientBuilder {
            this.secret = secret
            return this
        }

        fun withDebug(debug: Boolean): CloudbreakClientBuilder {
            this.debug = debug
            return this
        }

        fun withCertificateValidation(secure: Boolean): CloudbreakClientBuilder {
            this.secure = secure
            return this
        }


        fun build(): CloudbreakClient {
            val configKey = ConfigKey(secure, debug)
            if (secret != null) {
                return CloudbreakClient(cloudbreakAddress, identityServerAddress, secret, clientId, configKey)
            } else {
                return CloudbreakClient(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey)
            }
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CloudbreakClient::class.java)

        private val EMPTY_FORM = Form()
        private val TOKEN_KEY = "TOKEN"
        private val TOKEN_EXPIRATION_FACTOR = 0.9
    }

}
