package com.sequenceiq.cloudbreak.client

import java.security.KeyManagementException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import org.glassfish.jersey.SslConfigurator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CertificateTrustManager {

    private val LOGGER = LoggerFactory.getLogger(CertificateTrustManager::class.java)

    fun hostnameVerifier(): HostnameVerifier {

        // Do not verify host names
        return HostnameVerifier { hostname, sslSession ->
            LOGGER.info("verify hostname: {}", hostname)
            true
        }

    }


    fun sslContext(): SSLContext {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                LOGGER.info("accept all issuer")
                return null
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                LOGGER.info("checkClientTrusted")
                // Trust everything
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                LOGGER.info("checkServerTrusted")
                // Trust everything
            }
        })
        try {
            // Install the all-trusting trust manager
            val sc = SslConfigurator.newInstance().createSSLContext()
            sc.init(null, trustAllCerts, SecureRandom())
            LOGGER.warn("Trust all SSL cerificates has been installed")
            return sc
        } catch (e: KeyManagementException) {
            LOGGER.error(e.message, e)
            throw RuntimeException("F", e)
        }

    }

}