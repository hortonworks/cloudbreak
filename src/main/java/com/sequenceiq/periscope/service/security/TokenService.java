package com.sequenceiq.periscope.service.security;

import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

@Service
public class TokenService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(TokenService.class);

    @Autowired
    private RestOperations restTemplate;

    @Autowired
    @Qualifier("identityServerUrl")
    private String identityServerUrl;

    @Value("${periscope.client.id}")
    private String id;

    @Value("${periscope.client.secret}")
    private String secret;

    @SuppressWarnings("unchecked")
    @Cacheable("clientCache")
    public String getToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Basic " + Base64.encodeBase64String((id + ":" + secret).getBytes()));
        LOGGER.info(-1, "Requesting client credentials token");
        Map<String, String> tokenResponse = restTemplate.exchange(
                identityServerUrl + "/oauth/token?grant_type=client_credentials",
                HttpMethod.POST,
                new HttpEntity<Map>(null, headers),
                Map.class
        ).getBody();
        return tokenResponse.get("access_token");
    }

}
