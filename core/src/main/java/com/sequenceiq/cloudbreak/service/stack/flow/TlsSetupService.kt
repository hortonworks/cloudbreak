package com.sequenceiq.cloudbreak.service.stack.flow

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.concurrent.TimeUnit

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.google.common.io.BaseEncoding
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.SecurityConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter
import com.sequenceiq.cloudbreak.util.FileReaderUtils

import freemarker.template.Configuration
import freemarker.template.TemplateException
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile

@Component
class TlsSetupService {

    @Inject
    private val connector: ServiceProviderConnectorAdapter? = null
    @Inject
    private val securityConfigRepository: SecurityConfigRepository? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val stackRepository: StackRepository? = null
    @Inject
    private val sshCheckerTaskContextPollingService: PollingService<SshCheckerTaskContext>? = null
    @Inject
    private val sshCheckerTask: SshCheckerTask? = null
    @Inject
    private val freemarkerConfiguration: Configuration? = null
    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null

    @Value("#{'${cb.cert.dir:}/${cb.tls.cert.file:}'}")
    private val tlsCertificatePath: String? = null

    @Throws(CloudbreakException::class)
    fun setupTls(stack: Stack, publicIp: String?, sshPort: Int, user: String, sshFingerprints: Set<String>) {
        LOGGER.info("SSHClient parameters: stackId: {}, publicIp: {},  user: {}", stack.id, publicIp, user)
        if (publicIp == null) {
            throw CloudbreakException("Failed to connect to host, IP address not defined.")
        }
        val ssh = SSHClient()
        val orchestrator = stack.orchestrator
        val privateKeyLocation = tlsSecurityService!!.getSshPrivateFileLocation(stack.id)
        val hostKeyVerifier = VerboseHostKeyVerifier(sshFingerprints)
        try {
            waitForSsh(stack, publicIp, sshPort, hostKeyVerifier, user, privateKeyLocation)
            setupTemporarySsh(ssh, publicIp, sshPort, hostKeyVerifier, user, privateKeyLocation, stack.credential)
            uploadTlsSetupScript(orchestrator, ssh, publicIp, stack.gatewayPort, stack.credential)
            executeTlsSetupScript(ssh)
            removeTemporarySShKey(ssh, user, stack.credential)
            downloadAndSavePrivateKey(stack, ssh)
        } catch (e: IOException) {
            throw CloudbreakException("Failed to setup TLS through temporary SSH.", e)
        } catch (e: TemplateException) {
            throw CloudbreakException("Failed to generate TLS setup script.", e)
        } finally {
            try {
                ssh.disconnect()
            } catch (e: IOException) {
                throw CloudbreakException(String.format("Couldn't disconnect temp SSH session"), e)
            }

        }
    }

    private fun waitForSsh(stack: Stack, publicIp: String, sshPort: Int, hostKeyVerifier: HostKeyVerifier, user: String, privateKeyLocation: String) {
        sshCheckerTaskContextPollingService!!.pollWithTimeoutSingleFailure(
                sshCheckerTask,
                SshCheckerTaskContext(stack, hostKeyVerifier, publicIp, sshPort, user, tlsSecurityService!!.getSshPrivateFileLocation(stack.id)),
                SSH_POLLING_INTERVAL,
                SSH_MAX_ATTEMPTS_FOR_HOSTS)
    }

    @Throws(IOException::class)
    private fun setupTemporarySsh(ssh: SSHClient, ip: String, port: Int, hostKeyVerifier: HostKeyVerifier, user: String, privateKeyLocation: String,
                                  credential: Credential) {
        LOGGER.info("Setting up temporary ssh...")
        ssh.addHostKeyVerifier(hostKeyVerifier)
        ssh.connect(ip, port)
        if (credential.passwordAuthenticationRequired()) {
            ssh.authPassword(user, credential.loginPassword)
        } else {
            ssh.authPublickey(user, privateKeyLocation)
        }
        val remoteTlsCertificatePath = "/tmp/cb-client.pem"
        ssh.newSCPFileTransfer().upload(tlsCertificatePath, remoteTlsCertificatePath)
        LOGGER.info("Temporary ssh setup finished succesfully, public key is uploaded to {}", remoteTlsCertificatePath)
    }

    @Throws(IOException::class, TemplateException::class, CloudbreakException::class)
    private fun uploadTlsSetupScript(orchestrator: Orchestrator, ssh: SSHClient, publicIp: String, sslPort: Int?, credential: Credential) {
        LOGGER.info("Uploading tls-setup.sh to the gateway...")
        val model = HashMap<String, Any>()
        model.put("publicIp", publicIp)
        model.put("username", credential.loginUserName)
        model.put("sudopre", if (credential.passwordAuthenticationRequired()) String.format("echo '%s'|", credential.loginPassword) else "")
        model.put("sudocheck", if (credential.passwordAuthenticationRequired()) "-S" else "")
        model.put("sslPort", sslPort!!.toString())


        val type = orchestratorTypeResolver!!.resolveType(orchestrator.type)

        val tls = processTemplateIntoString(
                freemarkerConfiguration!!.getTemplate(String.format("init/%s/tls-setup.sh", type.name.toLowerCase()), "UTF-8"), model)
        val tlsFile = uploadParameterFile(tls, "tls-setup.sh")
        ssh.newSCPFileTransfer().upload(tlsFile, "/tmp/tls-setup.sh")
        LOGGER.info("tls-setup.sh uploaded to /tmp/tls-setup.sh. Content: {}", tls)

        if (type.hostOrchestrator()) {
            val nginxConf = FileReaderUtils.readFileFromClasspath("init/host/nginx.conf")
            val nginxConfFile = uploadParameterFile(nginxConf, "nginx.conf")
            ssh.newSCPFileTransfer().upload(nginxConfFile, "/tmp/nginx.conf")
            LOGGER.info("nginx conf uploaded to /tmp/nginx.conf. Content: {}", nginxConf)

            val notAv = FileReaderUtils.readFileFromClasspath("init/host/50x.json")
            val notAvFile = uploadParameterFile(notAv, "50x.json")
            ssh.newSCPFileTransfer().upload(notAvFile, "/tmp/50x.json")
            LOGGER.info("ngingx error page uploaded to /tmp/50x.json. Content: {}", notAv)
        }
    }

    private fun uploadParameterFile(generatedTemplate: String, name: String): InMemorySourceFile {
        val tlsScriptBytes = generatedTemplate.toByteArray(StandardCharsets.UTF_8)
        return object : InMemorySourceFile() {
            override fun getName(): String {
                return name
            }

            override fun getLength(): Long {
                return tlsScriptBytes.size.toLong()
            }

            @Throws(IOException::class)
            override fun getInputStream(): InputStream {
                return ByteArrayInputStream(tlsScriptBytes)
            }
        }
    }

    @Throws(IOException::class, CloudbreakException::class)
    private fun executeTlsSetupScript(ssh: SSHClient) {
        LOGGER.info("Executing tls-setup.sh on the gateway...")
        val exitStatus = executeSshCommand(ssh, "bash /tmp/tls-setup.sh", true, "tls-setup")
        LOGGER.info("tls-setup.sh finished with {} exitcode.", exitStatus)
        if (exitStatus != 0) {
            throw CloudbreakException(String.format("TLS setup script exited with error code: %s", exitStatus))
        }
    }

    @Throws(IOException::class, CloudbreakException::class)
    private fun removeTemporarySShKey(ssh: SSHClient, user: String, credential: Credential) {
        if (!credential.passwordAuthenticationRequired()) {
            LOGGER.info("Removing temporary sshkey from the gateway...")
            val removeCommand = String.format("sudo sed -i '/#tmpssh_start/,/#tmpssh_end/{s/./ /g}' /home/%s/.ssh/authorized_keys", user)
            val exitStatus = executeSshCommand(ssh, removeCommand, false, "")
            LOGGER.info("Temporary sshkey removed from the gateway, exitcode: {}", exitStatus)
            if (exitStatus != 0) {
                throw CloudbreakException(String.format("Failed to remove temp SSH key. Error code: %s", exitStatus))
            }
        }
    }

    @Throws(IOException::class, CloudbreakSecuritySetupException::class)
    private fun downloadAndSavePrivateKey(stack: Stack, ssh: SSHClient) {
        ssh.newSCPFileTransfer().download("/tmp/server.pem", tlsSecurityService!!.getCertDir(stack.id) + "/ca.pem")
        val stackWithSecurity = stackRepository!!.findByIdWithSecurityConfig(stack.id)
        val securityConfig = stackWithSecurity.securityConfig
        securityConfig.serverCert = BaseEncoding.base64().encode(tlsSecurityService.readServerCert(stack.id).toByteArray())
        securityConfigRepository!!.save(securityConfig)
    }

    @Throws(IOException::class)
    private fun startSshSession(ssh: SSHClient): Session {
        val sshSession = ssh.startSession()
        sshSession.allocateDefaultPTY()
        return sshSession
    }

    @Throws(IOException::class)
    private fun executeSshCommand(ssh: SSHClient, command: String, logOutput: Boolean, logPrefix: String): Int {
        val session = startSshSession(ssh)
        val cmd = session.exec(command)
        if (logOutput) {
            logStdOutAndStdErr(cmd, logPrefix)
        }
        cmd.join(SETUP_TIMEOUT.toLong(), TimeUnit.SECONDS)
        session.close()
        return cmd.exitStatus!!
    }

    @Throws(IOException::class)
    private fun logStdOutAndStdErr(command: Session.Command, commandDesc: String) {
        LOGGER.info("Standard output of {} command", commandDesc)
        LOGGER.info(String(IOUtils.readFully(command.inputStream).toString()))
        LOGGER.info("Standard error of {} command", commandDesc)
        LOGGER.info(String(IOUtils.readFully(command.errorStream).toString()))
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(TlsSetupService::class.java)
        private val SETUP_TIMEOUT = 180
        private val SSH_POLLING_INTERVAL = 5000
        private val SSH_MAX_ATTEMPTS_FOR_HOSTS = 100
    }
}
