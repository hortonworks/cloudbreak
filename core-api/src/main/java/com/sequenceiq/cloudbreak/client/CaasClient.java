package com.sequenceiq.cloudbreak.client;

import static javax.ws.rs.client.Entity.entity;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;

public class CaasClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaasClient.class);

    private final String caasProtocol;

    private final String caasDomain;

    private final ConfigKey configKey;

    public CaasClient(String caasProtocol, String caasDomain, ConfigKey configKey) {
        this.caasProtocol = caasProtocol;
        this.caasDomain = caasDomain;
        this.configKey = configKey;
    }

    public String getCaasProtocol() {
        return caasProtocol;
    }

    public String getCaasDomain() {
        return caasDomain;
    }

    @Cacheable(cacheNames = "caasUserCache")
    public CaasUser getUserInfo(String dpsJwtToken) {
        WebTarget caasWebTarget = getCaasWebTarget();
        WebTarget userInfoWebTarget = caasWebTarget.path("/oidc/userinfo");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Cookie", "dps-jwt=" + dpsJwtToken);
        return userInfoWebTarget.request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers)
                .get(CaasUser.class);
    }

    public IntrospectResponse introSpect(String dpsJwtToken) {
        WebTarget caasWebTarget = getCaasWebTarget();
        WebTarget introspectWebTarget = caasWebTarget.path("/oidc/introspect");
        try {
            return introspectWebTarget.request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(new IntrospectRequest(dpsJwtToken)), IntrospectResponse.class);
        } catch (ForbiddenException e) {
            throw new InvalidTokenException("Token is not valid", e);
        }
    }

    public String getAccessToken(TokenRequest refreshToken) {
        try (Response response = getCaasWebTarget().path("/oidc/token").request().post(entity(refreshToken, MediaType.APPLICATION_JSON_TYPE))) {
            return response.readEntity(TokenResponse.class).getAccessToken();
        }
    }

    private WebTarget getCaasWebTarget() {
        if (StringUtils.isNotEmpty(caasDomain)) {
            return RestClientUtil.get(configKey).target(caasProtocol + "://" + caasDomain);
        } else {
            LOGGER.warn("CAAS isn't configured");
            throw new InvalidTokenException("CAAS isn't configured");
        }
    }

}