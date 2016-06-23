package com.sequenceiq.cloudbreak.service

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import javax.inject.Inject

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.google.common.io.BaseEncoding
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.KeyPair
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.SecurityConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig
import com.sequenceiq.cloudbreak.util.FileReaderUtils

@Component
class TlsSecurityService {

    @Value("${cb.cert.dir:}")
    private val certDir: String? = null

    @Value("#{'${cb.cert.dir:}/${cb.tls.cert.file:}'}")
    private val clientCert: String? = null

    @Value("#{'${cb.cert.dir:}/${cb.tls.private.key.file:}'}")
    private val clientPrivateKey: String? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val securityConfigRepository: SecurityConfigRepository? = null

    @Throws(CloudbreakSecuritySetupException::class)
    fun storeSSHKeys(stack: Stack) {
        try {
            generateTempSshKeypair(stack.id)
            val securityConfig = SecurityConfig()
            securityConfig.clientKey = BaseEncoding.base64().encode(readClientKey(stack.id).toByteArray())
            securityConfig.clientCert = BaseEncoding.base64().encode(readClientCert(stack.id).toByteArray())
            securityConfig.temporarySshPrivateKey = BaseEncoding.base64().encode(readPrivateSshKey(stack.id).toByteArray())
            securityConfig.temporarySshPublicKey = BaseEncoding.base64().encode(readPublicSshKey(stack.id).toByteArray())
            securityConfig.stack = stack
            securityConfigRepository!!.save(securityConfig)
        } catch (e: IOException) {
            throw CloudbreakSecuritySetupException("Failed to generate temporary SSH key pair.", e)
        } catch (e: JSchException) {
            throw CloudbreakSecuritySetupException("Failed to generate temporary SSH key pair.", e)
        }

    }

    fun getCertDir(stackId: Long?): String {
        return Paths.get(certDir + "/stack-" + stackId).toString()
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun prepareCertDir(stackId: Long?): String {
        val stackCertDir = Paths.get(getCertDir(stackId))
        if (!Files.exists(stackCertDir)) {
            try {
                LOGGER.info("Creating directory for the keys and certificates under {}", certDir)
                Files.createDirectories(stackCertDir)
                prepareFiles(stackId)
            } catch (se: IOException) {
                throw CloudbreakSecuritySetupException("Failed to create directory: " + stackCertDir)
            } catch (se: SecurityException) {
                throw CloudbreakSecuritySetupException("Failed to create directory: " + stackCertDir)
            }

        } else {
            prepareFiles(stackId)
        }
        return stackCertDir.toString()
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun prepareFiles(stackId: Long?) {
        val stack = stackRepository!!.findByIdWithSecurityConfig(stackId)
        if (stack != null && stack.securityConfig != null) {
            val id = stack.id
            readServerCert(id)
            readClientCert(id)
            readClientKey(id)
            readPrivateSshKey(id)
            readPublicSshKey(id)
        }
    }

    fun getSshPrivateFileLocation(stackId: Long?): String {
        return Paths.get(getCertDir(stackId) + "/" + getPrivateSshKeyFileName(stackId)).toString()
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun readSecurityFile(stackId: Long?, fileName: String): String {
        try {
            return FileReaderUtils.readFileFromPathToString(Paths.get(getCertDir(stackId) + "/" + fileName).toString())
        } catch (se: IOException) {
            throw CloudbreakSecuritySetupException("Failed to read file: " + getCertDir(stackId) + "/" + fileName)
        } catch (se: SecurityException) {
            throw CloudbreakSecuritySetupException("Failed to read file: " + getCertDir(stackId) + "/" + fileName)
        }

    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun writeSecurityFile(stackId: Long?, content: String?, fileName: String) {
        try {
            val path = Paths.get(getCertDir(stackId) + "/" + fileName).toString()
            val directory = File(getCertDir(stackId))
            if (!directory.exists()) {
                Files.createDirectories(Paths.get(getCertDir(stackId)))
            }
            val file = File(path)
            if (!file.exists()) {
                if (content != null) {
                    val output = FileOutputStream(file)
                    IOUtils.write(Base64.decodeBase64(content), output)
                }
            }
        } catch (se: IOException) {
            throw CloudbreakSecuritySetupException("Failed to write file: " + getCertDir(stackId) + "/" + fileName)
        } catch (se: SecurityException) {
            throw CloudbreakSecuritySetupException("Failed to write file: " + getCertDir(stackId) + "/" + fileName)
        }

    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun checkSecurityFileExist(stackId: Long?, fileName: String): Boolean {
        try {
            val path = Paths.get(getCertDir(stackId) + "/" + fileName).toString()
            val directory = File(getCertDir(stackId))
            if (!directory.exists()) {
                return false
            }
            val file = File(path)
            if (!file.exists()) {
                return false
            }
        } catch (se: SecurityException) {
            throw CloudbreakSecuritySetupException("Failed to check file: " + getCertDir(stackId) + "/" + fileName)
        }

        return true
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun copyClientKeys(stackId: Long?) {
        try {
            val stackCertDir = Paths.get(getCertDir(stackId))
            val file = File(stackCertDir.toString())
            if (!file.exists()) {
                Files.createDirectories(stackCertDir)
            }
            Files.copy(Paths.get(clientPrivateKey), Paths.get(stackCertDir + "/key.pem"), StandardCopyOption.REPLACE_EXISTING)
            Files.copy(Paths.get(clientCert), Paths.get(stackCertDir + "/cert.pem"), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw CloudbreakSecuritySetupException(String.format("Failed to copy client certificate to certificate directory." + " Check if '%s' and '%s' exist.", clientCert, clientPrivateKey), e)
        }

    }

    fun getPublicSshKeyFileName(stackId: Long?): String {
        return SSH_KEY_PREFIX + stackId + SSH_PUBLIC_KEY_EXTENSION
    }

    fun getPrivateSshKeyFileName(stackId: Long?): String {
        return SSH_KEY_PREFIX + stackId!!
    }

    @Throws(JSchException::class, IOException::class)
    fun generateTempSshKeypair(stackId: Long?): String {
        LOGGER.info("Generating temporary SSH keypair.")
        val publicKeyPath = getCertDir(stackId) + getPublicSshKeyFileName(stackId)
        val privateKeyPath = getCertDir(stackId) + getPrivateSshKeyFileName(stackId)
        val jsch = JSch()
        val keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, DEFAULT_KEY_SIZE)
        keyPair.writePrivateKey(privateKeyPath)
        keyPair.writePublicKey(publicKeyPath, SSH_PUBLIC_KEY_COMMENT)
        keyPair.dispose()
        LOGGER.info("Generated temporary SSH keypair: {}. Fingerprint: {}", privateKeyPath, keyPair.fingerPrint)
        return privateKeyPath
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun buildGatewayConfig(stackId: Long?, publicIp: String, gatewayPort: Int?,
                           privateIp: String, hostname: String): GatewayConfig {
        prepareCertDir(stackId)
        val conf = buildTLSClientConfig(stackId, publicIp)
        return GatewayConfig(publicIp, privateIp, hostname, gatewayPort,
                prepareCertDir(stackId), conf.serverCert, conf.clientCert, conf.clientKey)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun buildTLSClientConfig(stackId: Long?, apiAddress: String): HttpClientConfig {
        val stack = stackRepository!!.findOneWithLists(stackId)
        if (stack.isInstanceGroupsSpecified) {
            prepareCertDir(stackId)
            return HttpClientConfig(apiAddress, stack.gatewayPort, prepareCertDir(stackId))
        } else {
            return HttpClientConfig(apiAddress, stack.gatewayPort)
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun readClientKey(stackId: Long?): String {
        val stack = stackRepository!!.findByIdWithSecurityConfig(stackId)
        if (!checkSecurityFileExist(stackId, "key.pem")) {
            writeSecurityFile(stackId, stack.securityConfig.clientKey, "key.pem")
        }
        return readSecurityFile(stackId, "key.pem")
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun readClientCert(stackId: Long?): String {
        val stack = stackRepository!!.findByIdWithSecurityConfig(stackId)
        if (!checkSecurityFileExist(stackId, "cert.pem")) {
            writeSecurityFile(stackId, stack.securityConfig.clientCert, "cert.pem")
        }
        return readSecurityFile(stackId, "cert.pem")
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun readServerCert(stackId: Long?): String {
        val stack = stackRepository!!.findByIdWithSecurityConfig(stackId)
        if (!checkSecurityFileExist(stackId, "ca.pem")) {
            writeSecurityFile(stackId, stack.securityConfig.serverCert, "ca.pem")
        }
        return readSecurityFile(stackId, "ca.pem")
    }


    @Throws(CloudbreakSecuritySetupException::class)
    fun readPrivateSshKey(stackId: Long?): String {
        val stack = stackRepository!!.findByIdWithSecurityConfig(stackId)
        if (!checkSecurityFileExist(stackId, getPrivateSshKeyFileName(stackId))) {
            writeSecurityFile(stackId, stack.securityConfig.temporarySshPrivateKey, getPrivateSshKeyFileName(stackId))
        }
        return readSecurityFile(stackId, getPrivateSshKeyFileName(stackId))
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun readPublicSshKey(stackId: Long?): String {
        val stack = stackRepository!!.findByIdWithSecurityConfig(stackId)
        if (!checkSecurityFileExist(stackId, getPublicSshKeyFileName(stackId))) {
            writeSecurityFile(stackId, stack.securityConfig.temporarySshPublicKey, getPublicSshKeyFileName(stackId))
        }
        return readSecurityFile(stackId, getPublicSshKeyFileName(stackId))
    }

    fun getCertificate(id: Long?): ByteArray {
        val cert = securityConfigRepository!!.getServerCertByStackId(id) ?: throw NotFoundException("Stack doesn't exist, or certificate was not found for stack.")
        return Base64.decodeBase64(cert)
    }

    companion object {

        val SSH_PUBLIC_KEY_EXTENSION = ".pub"

        private val LOGGER = LoggerFactory.getLogger(TlsSecurityService::class.java)
        private val SSH_PUBLIC_KEY_COMMENT = "cloudbreak"
        private val DEFAULT_KEY_SIZE = 2048
        private val SSH_KEY_PREFIX = "/cb-ssh-key-"
    }

}
