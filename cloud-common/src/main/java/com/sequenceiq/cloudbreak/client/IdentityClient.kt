package com.sequenceiq.cloudbreak.client

import javax.ws.rs.core.Response.Status.fromStatusCode

import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.net.ssl.SSLHandshakeException
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import org.apache.commons.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IdentityClient(private val identityServerAddress: String, private val clientId: String, configKey: ConfigKey) {

    private val authorizeWebTarget: WebTarget

    private val tokenWebTarget: WebTarget

    init {
        val identityWebTarget = RestClientUtil.get(configKey).target(identityServerAddress)
        authorizeWebTarget = identityWebTarget.path("/oauth/authorize").queryParam("response_type", "token").queryParam("client_id", clientId)
        tokenWebTarget = identityWebTarget.path("/oauth/token").queryParam("grant_type", "client_credentials")
        LOGGER.info("IdentityClient has been created. identity: {}, clientId: {}, configKey: {}", identityServerAddress, clientId, configKey)
    }

    fun getToken(user: String, password: String): AccessToken {
        val formData = MultivaluedHashMap<String, String>()
        formData.add("credentials", String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, password))
        try {
            val resp = authorizeWebTarget.request().accept(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(Entity.form(formData))
            val token: String
            val exp: Int
            when (fromStatusCode(resp.status)) {
                Response.Status.FOUND -> {
                    val location = resp.getHeaderString("Location")
                    val m = LOCATION_PATTERN.matcher(location)
                    if (m.matches()) {
                        token = m.group(1)
                        exp = Integer.parseInt(m.group(2))

                    } else {
                        throw TokenUnavailableException(String.format("Failed to parse access token from the identity server,  check its configuration! " + "Raw Location response: %s", location))
                    }
                }
                else -> throw TokenUnavailableException(String.format("Couldn't get an access token from the identity server, check its configuration!" + " Perhaps %s is not autoapproved? Response headers: %s", clientId, resp.headers))
            }
            return AccessToken(token, "bearer", exp)
        } catch (e: ProcessingException) {
            if (e.cause is SSLHandshakeException) {
                throw SSLConnectionException(String.format("Failed to connect (%s) due to SSL handshake error.",
                        identityServerAddress), e)
            }
            throw TokenUnavailableException("Error occurred while getting token from identity server", e)
        } catch (e: TokenUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw TokenUnavailableException("Error occurred while getting token from identity server", e)
        }

    }

    fun getToken(secret: String): AccessToken {
        try {
            val headers = MultivaluedHashMap<String, Any>()
            headers.add("Authorization", "Basic " + Base64.encodeBase64String((clientId + ":" + secret).toByteArray()))
            return tokenWebTarget.request().accept(MediaType.APPLICATION_JSON_TYPE).headers(headers).post<AccessToken>(Entity.json<Any>(null), AccessToken::class.java)
        } catch (e: Exception) {
            throw TokenUnavailableException("Error occurred while getting token from identity server", e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(IdentityClient::class.java)

        private val LOCATION_PATTERN = Pattern.compile(".*access_token=(.*)\\&expires_in=(\\d*)\\&scope=.*")
    }

}