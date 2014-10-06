package com.sequenceiq.periscope.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

@Service
public class TokenService {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(TokenService.class);
    private static final String CLIENT_ID = "periscope_client";

    @Value("${periscope.identity.host}")
    private String identityServerHost;
    @Value("${periscope.identity.port}")
    private String identityServerPort;
    @Value("${periscope.cloudbreak.user}")
    private String user;
    @Value("${periscope.cloudbreak.pass}")
    private String pass;

    public String getToken() throws TokenUnavailableException {
        String identityServerUrl = String.format("http://%s:%s", identityServerHost, identityServerPort);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("credentials", String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, pass));

        String token;
        try {
            ResponseEntity<String> authResponse = restTemplate.exchange(
                    String.format("%s/oauth/authorize?response_type=token&client_id=%s", identityServerUrl, CLIENT_ID),
                    HttpMethod.POST,
                    new HttpEntity<Map>(requestBody, headers),
                    String.class
            );
            if (HttpStatus.FOUND == authResponse.getStatusCode() && authResponse.getHeaders().get("Location") != null) {
                String location = authResponse.getHeaders().get("Location").get(0);
                String[] parts = location.split("#|&|=");
                token = parts[2];
            } else {
                LOGGER.info(LOGGER.NOT_CLUSTER_RELATED, "Couldn't get an access token from the identity server, check its configuration!");
                LOGGER.info(LOGGER.NOT_CLUSTER_RELATED, "Response from identity server: ");
                LOGGER.info(LOGGER.NOT_CLUSTER_RELATED, "Headers: " + authResponse);
                throw new TokenUnavailableException("Wrong response from identity server.");
            }
        } catch (Exception e) {
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        }
        return token;
    }
}
