package com.sequenceiq.cloudbreak.client;

import static javax.ws.rs.core.Response.Status.fromStatusCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);

    private static final Pattern LOCATION_PATTERN = Pattern.compile(".*access_token=(.*)\\&expires_in=(\\d*)\\&scope=.*");

    private final String identityServerAddress;

    private final String clientId;

    private final WebTarget authorizeWebTarget;

    private final WebTarget tokenWebTarget;

    public IdentityClient(String identityServerAddress, String clientId, ConfigKey configKey) {
        this.identityServerAddress = identityServerAddress;
        this.clientId = clientId;
        WebTarget identityWebTarget = RestClientUtil.get(configKey).target(identityServerAddress);
        authorizeWebTarget = identityWebTarget.path("/oauth/authorize").queryParam("response_type", "token").queryParam("client_id", clientId);
        tokenWebTarget = identityWebTarget.path("/oauth/token").queryParam("grant_type", "client_credentials");
        LOGGER.info("IdentityClient has been created. identity: {}, clientId: {}, configKey: {}", identityServerAddress, clientId, configKey);
    }

    public AccessToken getToken(String user, String password) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("credentials", String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, password));
        try {
            Response resp = authorizeWebTarget.request().accept(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(Entity.form(formData));
            String token;
            int exp;
            switch (fromStatusCode(resp.getStatus())) {
                case FOUND:
                    String location = resp.getHeaderString("Location");
                    Matcher m = LOCATION_PATTERN.matcher(location);
                    if (m.matches()) {
                        token = m.group(1);
                        exp = Integer.parseInt(m.group(2));

                    } else {
                        throw new TokenUnavailableException(String.format("Failed to parse access token from the identity server,  check its configuration! "
                                + "Raw Location response: %s", location));
                    }
                    break;
                default:
                    throw new TokenUnavailableException(String.format("Couldn't get an access token from the identity server, check its configuration!"
                            + " Perhaps %s is not autoapproved? Response headers: %s", clientId, resp.getHeaders()));
            }
            return new AccessToken(token, "bearer", exp);
        } catch (ProcessingException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                throw new SSLConnectionException(String.format("Failed to connect (%s) due to SSL handshake error.",
                        identityServerAddress), e);
            }
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        } catch (TokenUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        }
    }

    public AccessToken getToken(String secret) {
        try {
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add("Authorization", "Basic " + Base64.encodeBase64String((clientId + ":" + secret).getBytes()));
            return tokenWebTarget.request().accept(MediaType.APPLICATION_JSON_TYPE).headers(headers).post(Entity.json(null), AccessToken.class);
        } catch (Exception e) {
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        }
    }

}