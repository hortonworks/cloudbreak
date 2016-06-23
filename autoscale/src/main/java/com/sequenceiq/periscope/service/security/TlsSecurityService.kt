package com.sequenceiq.periscope.service.security

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.SecurityConfig
import com.sequenceiq.periscope.model.TlsConfiguration
import com.sequenceiq.periscope.repository.SecurityConfigRepository

@Service
class TlsSecurityService {

    @Value("${periscope.cert.dir:" + CERT_DIR + "} ")
    private val certDir: String? = null
    @Value("#{'${periscope.cert.dir:" + CERT_DIR + "} ' + '/' + '${periscope.tls.cert.file:client.pem}'}")
    private val clientCertName: String? = null
    @Value("#{'${periscope.cert.dir:" + CERT_DIR + "} ' + '/' + '${periscope.tls.private .key.file:client-key.pem}'}")
    private val clientPrivateKeyName: String? = null

    @Inject
    private val cloudbreakClient: CloudbreakClient? = null
    @Inject
    private val securityConfigRepository: SecurityConfigRepository? = null

    fun prepareSecurityConfig(stackId: Long?): SecurityConfig {
        val stackCertDir = getCertDir(stackId)
        if (!Files.exists(stackCertDir)) {
            try {
                LOGGER.info("Creating directory for the certificates: {}", stackCertDir)
                Files.createDirectory(stackCertDir)
            } catch (e: IOException) {
                throw TlsConfigurationException("Failed to create directory " + stackCertDir, e)
            }

        }
        val clientKeyDst = stackCertDir.resolve(KEY_FILE)
        val clientCertDst = stackCertDir.resolve(CERT_FILE)
        try {
            Files.copy(Paths.get(clientPrivateKeyName), clientKeyDst, StandardCopyOption.REPLACE_EXISTING)
            Files.copy(Paths.get(clientCertName), clientCertDst, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw TlsConfigurationException("Failed to copy client certificate to " + stackCertDir, e)
        }

        val serverCert: ByteArray

        try {
            serverCert = cloudbreakClient!!.stackEndpoint().getCertificate(stackId).certificate
            Files.write(stackCertDir.resolve(SERVER_CERT_FILE), serverCert, StandardOpenOption.CREATE)
        } catch (e: Exception) {
            throw TlsConfigurationException("Failed to write server certificate to " + stackCertDir, e)
        }

        val clientKey: ByteArray
        val clientCert: ByteArray

        try {
            clientKey = Files.readAllBytes(clientKeyDst)
            clientCert = Files.readAllBytes(clientCertDst)
        } catch (e: IOException) {
            throw TlsConfigurationException("Failed to read client certificate file from " + stackCertDir, e)
        }

        return SecurityConfig(clientKey, clientCert, serverCert)
    }

    fun getConfiguration(cluster: Cluster): TlsConfiguration {
        val certDir = getCertDir(cluster.stackId)
        val clientKeyPath = certDir.resolve(KEY_FILE)
        val clientCertPath = certDir.resolve(CERT_FILE)
        val serverCertPath = certDir.resolve(SERVER_CERT_FILE)
        try {
            if (!Files.exists(certDir)) {
                LOGGER.info("Recreating certificate directory [{}] because it doesn't exist.", certDir)
                Files.createDirectory(certDir)
            }
            if (!Files.exists(clientKeyPath) || !Files.exists(clientCertPath) || !Files.exists(serverCertPath)) {
                LOGGER.info("Recreating certificate files in {} because they don't exist.", certDir)
                val securityConfig = securityConfigRepository!!.findByClusterId(cluster.id)
                Files.write(clientKeyPath, securityConfig.clientKey, StandardOpenOption.CREATE)
                Files.write(clientCertPath, securityConfig.clientCert, StandardOpenOption.CREATE)
                Files.write(serverCertPath, securityConfig.serverCert, StandardOpenOption.CREATE)
            }
        } catch (e: IOException) {
            throw TlsConfigurationException("Failed to write certificates to file.", e)
        }

        return TlsConfiguration(clientKeyPath.toString(), clientCertPath.toString(), serverCertPath.toString())
    }

    private fun getCertDir(stackId: Long?): Path {
        return Paths.get(certDir + DIR_PREFIX + stackId)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(TlsSecurityService::class.java)
        private val CERT_DIR = "/certs"
        private val KEY_FILE = "key.pem"
        private val CERT_FILE = "cert.pem"
        private val SERVER_CERT_FILE = "ca.pem"
        private val DIR_PREFIX = "/stack-"
    }
}
