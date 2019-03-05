package com.sequenceiq.cloudbreak.client;

import static javax.ws.rs.core.Response.Status.fromStatusCode;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.util.Assert;

public class IdentityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);

    private static final Pattern LOCATION_PATTERN = Pattern.compile(".*access_token=(.*)\\&expires_in=(\\d*)\\&scope=.*");

    private final String identityServerAddress;

    private final String clientId;

    private final WebTarget authorizeWebTarget;

    private final WebTarget checkTokenWebTarget;

    private final WebTarget tokenWebTarget;

    public IdentityClient(String identityServerAddress, String clientId, ConfigKey configKey) {
        this.identityServerAddress = identityServerAddress;
        this.clientId = clientId;
        WebTarget identityWebTarget = RestClientUtil.get(configKey).target(identityServerAddress);
        authorizeWebTarget = identityWebTarget.path("/oauth/authorize").queryParam("response_type", "token").queryParam("client_id", clientId);
        tokenWebTarget = identityWebTarget.path("/oauth/token").queryParam("grant_type", "client_credentials");
        checkTokenWebTarget = identityWebTarget.path("/check_token");
        LOGGER.debug("IdentityClient has been created. identity: {}, clientId: {}, configKey: {}", identityServerAddress, clientId, configKey);
    }

    public AccessToken getToken(String user, String password) {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("credentials", String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, password.replace("\\", "\\\\").replace("\"", "\\\"")));
        try (Response resp = authorizeWebTarget.request().accept(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(Entity.form(formData))) {
            String token;
            int exp;
            if (fromStatusCode(resp.getStatus()) == Status.FOUND) {
                String location = resp.getHeaderString("Location");
                Matcher m = LOCATION_PATTERN.matcher(location);
                if (m.matches()) {
                    token = m.group(1);
                    exp = Integer.parseInt(m.group(2));

                } else {
                    throw new TokenUnavailableException(String.format("Failed to parse access token from the identity server,  check its configuration! "
                            + "Raw Location response: %s", location));
                }
            } else {
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
        } catch (RuntimeException e) {
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        }
    }

    public AccessToken getToken(String secret) {
        try {
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add("Authorization", "Basic " + Base64.encodeBase64String((clientId + ':' + secret).getBytes()));
            return tokenWebTarget.request().accept(MediaType.APPLICATION_JSON_TYPE).headers(headers).post(Entity.json(null), AccessToken.class);
        } catch (RuntimeException e) {
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        }
    }

    // Based on this implementation org.springframework.security.oauth2.provider.token.RemoteTokenServices because we need specific headers
    public Map<String, Object> loadAuthentication(String accessToken, String clientSecret) throws AuthenticationException, InvalidTokenException {
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        String tokenName = "token";
        formData.add(tokenName, accessToken);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Authorization", "Basic " + Base64.encodeBase64String((clientId + ':' + clientSecret).getBytes()));

        Map<String, Object> response;
        try {
            response = checkTokenWebTarget.request().accept(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    .headers(headers).post(Entity.form(formData), Map.class);
        } catch (BadRequestException ex) {
            LOGGER.debug(String.format("Token check failed for access token: '%s'.", accessToken), ex);
            throw new InvalidTokenException(accessToken);
        }

        if (response.containsKey("error")) {
            throw new InvalidTokenException(accessToken);
        }

        Assert.state(response.containsKey("client_id"), "Client id must be present in response from auth server");
        return response;
    }

    public Map<String, Object> readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported: read access token");
    }
}