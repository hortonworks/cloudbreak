package com.sequenceiq.cloudbreak.orchestrator.salt.client

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.stream.Collectors

import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Form
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import org.apache.http.HttpStatus
import org.glassfish.jersey.media.multipart.Boundary
import org.glassfish.jersey.media.multipart.FormDataMultiPart
import org.glassfish.jersey.media.multipart.MultiPart
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.client.RestClientUtil
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction

class SaltConnector(gatewayConfig: GatewayConfig, debug: Boolean) : Closeable {

    private val restClient: Client?
    private val saltTarget: WebTarget

    init {
        try {
            this.restClient = RestClientUtil.createClient(
                    gatewayConfig.serverCert, gatewayConfig.clientCert, gatewayConfig.clientKey, debug, SaltConnector::class.java)
            this.saltTarget = RestClientUtil.createTarget(restClient, gatewayConfig.gatewayUrl)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create rest client with 2-way-ssl config", e)
        }

    }

    fun health(): GenericResponse {
        val response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.contextPath).request().get().readEntity<GenericResponse>(GenericResponse::class.java)
        LOGGER.info("Health response: {}", response)
        return response
    }

    fun pillar(pillar: Pillar): GenericResponse {
        val response = saltTarget.path(SaltEndpoint.BOOT_PILLAR_SAVE.contextPath).request().post(Entity.json(pillar)).readEntity<GenericResponse>(GenericResponse::class.java)
        LOGGER.info("Pillar response: {}", response)
        return response
    }

    fun action(saltAction: SaltAction): GenericResponses {
        val responses = saltTarget.path(SaltEndpoint.BOOT_ACTION_DISTRIBUTE.contextPath).request().post(Entity.json(saltAction)).readEntity<GenericResponses>(GenericResponses::class.java)
        LOGGER.info("SaltAction response: {}", responses)
        return responses
    }


    fun <T> run(target: Target<String>, `fun`: String, clientType: SaltClientType, clazz: Class<T>, vararg arg: String): T {
        var form = Form()
        form = addAuth(form).param("fun", `fun`).param("client", clientType.type).param("tgt", target.target).param("expr_form", target.type)
        if (arg != null) {
            if (clientType == SaltClientType.LOCAL || clientType == SaltClientType.LOCAL_ASYNC) {
                for (a in arg) {
                    form.param("arg", a)
                }
            } else {
                var i = 0
                while (i < arg.size - 1) {
                    form.param(arg[i], arg[i + 1])
                    i = i + 2
                }
            }
        }
        val response = saltTarget.path(SaltEndpoint.SALT_RUN.contextPath).request().post(Entity.form(form)).readEntity(clazz)
        LOGGER.info("Salt run response: {}", response)
        return response
    }

    fun <T> wheel(`fun`: String, match: Collection<String>?, clazz: Class<T>): T {
        var form = Form()
        form = addAuth(form).param("fun", `fun`).param("client", "wheel")
        if (match != null && !match.isEmpty()) {
            form.param("match", match.stream().collect(Collectors.joining(",")))
        }
        val response = saltTarget.path(SaltEndpoint.SALT_RUN.contextPath).request().post(Entity.form(form)).readEntity(clazz)
        LOGGER.info("SaltAction response: {}", response)
        return response
    }

    @Throws(IOException::class)
    fun upload(path: String, fileName: String, inputStream: InputStream) {
        val streamDataBodyPart = StreamDataBodyPart("file", inputStream, fileName)
        val multiPart = FormDataMultiPart().field("path", path).bodyPart(streamDataBodyPart)
        var contentType = MediaType.MULTIPART_FORM_DATA_TYPE
        contentType = Boundary.addBoundary(contentType)
        val response = saltTarget.path(SaltEndpoint.BOOT_FILE_UPLOAD.contextPath).request().post(Entity.entity(multiPart, contentType))
        if (response.status != HttpStatus.SC_OK) {
            throw IOException("can't upload file, status code: " + response.status)
        }
    }

    private fun addAuth(form: Form): Form {
        form.param("username", SALT_USER).param("password", SALT_PASSWORD).param("eauth", "pam")
        return form
    }

    @Throws(IOException::class)
    override fun close() {
        restClient?.close()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SaltConnector::class.java)

        private val SALT_USER = "saltuser"
        private val SALT_PASSWORD = "saltpass"
    }
}
