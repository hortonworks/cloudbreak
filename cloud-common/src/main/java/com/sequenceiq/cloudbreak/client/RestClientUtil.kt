package com.sequenceiq.cloudbreak.client

import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory.LOGGER

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import javax.net.ssl.SSLContext
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget

import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContexts
import org.glassfish.jersey.apache.connector.ApacheClientProperties
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.glassfish.jersey.filter.LoggingFilter
import org.glassfish.jersey.media.multipart.MultiPartFeature

object RestClientUtil {

    private val USER = "cbadmin"
    private val PASSWORD = "cbadmin"

    // apache http connection pool defaults are constraining
    // https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    private val MAX_TOTAL_CONNECTION = 100
    private val MAX_PER_ROUTE_CONNECTION = 20

    private val clients = ConcurrentHashMap<ConfigKey, Client>()

    @Synchronized fun get(): Client {
        return get(ConfigKey(false, false))
    }

    @Synchronized operator fun get(configKey: ConfigKey): Client {
        var client: Client? = clients[configKey]
        if (client == null) {
            client = createClient(configKey)
            clients.put(configKey, client)
        }
        LOGGER.info("RestClient cache size: {}, key: {}, fetched client: {}", clients.size, configKey, client)
        return client
    }

    @Throws(Exception::class)
    @JvmOverloads fun createClient(serverCert: String, clientCert: String, clientKey: String, debug: Boolean = false, debugClass: Class<Any>? = null): Client {
        val sslContext = SSLContexts.custom().loadTrustMaterial(KeyStoreUtil.createTrustStore(serverCert), null).loadKeyMaterial(KeyStoreUtil.createKeyStore(clientCert, clientKey), "consul".toCharArray()).build()

        LOGGER.info("Constructing jax rs client for config: server cert: {}, client cert: {}, debug: {}", serverCert, clientCert, debug)
        val config = ClientConfig()
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false")

        val registryBuilder = RegistryBuilder.create<ConnectionSocketFactory>()
        registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory())
        registryBuilder.register("https", SSLConnectionSocketFactory(sslContext))

        val connectionManager = PoolingHttpClientConnectionManager(registryBuilder.build())

        connectionManager.maxTotal = MAX_TOTAL_CONNECTION
        connectionManager.defaultMaxPerRoute = MAX_PER_ROUTE_CONNECTION

        // tell the jersey config about the connection manager
        config.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager)
        config.connectorProvider(ApacheConnectorProvider())
        config.register(MultiPartFeature::class.java)

        var builder = JerseyClientBuilder.newBuilder().withConfig(config)

        if (debug) {
            builder = builder.register(LoggingFilter(java.util.logging.Logger.getLogger(debugClass!!.name),
                    true))
        }

        val client = builder.build()
        LOGGER.info("Jax rs client has been constructed: {}, sslContext: {}", client, sslContext)
        return client
    }

    fun createTarget(client: Client, address: String): WebTarget {
        val feature = HttpAuthenticationFeature.basic(USER, PASSWORD)
        return client.target(address).register(feature)
    }

    private fun createClient(configKey: ConfigKey): Client {
        LOGGER.info("Constructing jax rs client: {}", configKey)
        val config = ClientConfig()
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false")

        val connectionManager: PoolingHttpClientConnectionManager

        if (configKey.isSecure) {
            connectionManager = PoolingHttpClientConnectionManager()
        } else {
            val socketFactoryRegistry = RegistryBuilder.create<ConnectionSocketFactory>().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", SSLConnectionSocketFactory(CertificateTrustManager.sslContext(), CertificateTrustManager.hostnameVerifier())).build()

            connectionManager = PoolingHttpClientConnectionManager(socketFactoryRegistry)
        }
        connectionManager.maxTotal = MAX_TOTAL_CONNECTION
        connectionManager.defaultMaxPerRoute = MAX_PER_ROUTE_CONNECTION

        // tell the jersey config about the connection manager
        config.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager)

        config.connectorProvider(ApacheConnectorProvider())
        config.register(MultiPartFeature::class.java)

        var builder = JerseyClientBuilder.newBuilder().withConfig(config)

        if (configKey.isDebug) {
            builder = builder.register(LoggingFilter(java.util.logging.Logger.getLogger(RestClientUtil::class.java!!.getName()), true))
        }

        val client = builder.build()

        val sslContext = client.sslContext
        LOGGER.warn("RestClient has been constructed: {}, client: {}, sslContext: {}", configKey, client, sslContext)
        return client
    }
}
