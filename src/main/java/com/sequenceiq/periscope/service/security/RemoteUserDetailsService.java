package com.sequenceiq.periscope.service.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    private static final int ACCOUNT_PART = 2;

    @Autowired
    private RestOperations restTemplate;

    @Value("${periscope.client.id}")
    private String clientId;

    @Value("${periscope.client.secret}")
    private String clientSecret;

    @Value("${periscope.identity.server.url}")
    private String identityServerUrl;

    @Override
    @Cacheable("userCache")
    @SuppressWarnings("unchecked")
    public PeriscopeUser getDetails(String filterValue, UserFilterField filterField) {
        HttpHeaders tokenRequestHeaders = new HttpHeaders();
        tokenRequestHeaders.set("Authorization", getAuthorizationHeader(clientId, clientSecret));

        Map<String, String> tokenResponse = restTemplate.exchange(
                identityServerUrl + "/oauth/token?grant_type=client_credentials",
                HttpMethod.POST,
                new HttpEntity<>(tokenRequestHeaders),
                Map.class).getBody();

        HttpHeaders scimRequestHeaders = new HttpHeaders();
        scimRequestHeaders.set("Authorization", "Bearer " + tokenResponse.get("access_token"));

        String scimResponse;

        switch (filterField) {
            case USERNAME:
                scimResponse = restTemplate.exchange(
                        identityServerUrl + "/Users/" + "?filter=userName eq \"" + filterValue + "\"",
                        HttpMethod.GET,
                        new HttpEntity<>(scimRequestHeaders),
                        String.class).getBody();
                break;
            case USER_ID:
                scimResponse = restTemplate.exchange(
                        identityServerUrl + "/Users/" + filterValue,
                        HttpMethod.GET,
                        new HttpEntity<>(scimRequestHeaders),
                        String.class).getBody();
                break;
            default:
                throw new UserDetailsUnavailableException("User details cannot be retrieved.");
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(scimResponse);
            JsonNode userNode = root;
            if (UserFilterField.USERNAME.equals(filterField)) {
                userNode = root.get("resources").get(0);
            }
            String account = null;
            for (Iterator<JsonNode> iterator = userNode.get("groups").getElements(); iterator.hasNext(); ) {
                JsonNode node = iterator.next();
                String group = node.get("display").asText();
                if (group.startsWith("sequenceiq.account")) {
                    String[] parts = group.split("\\.");
                    if (account != null && !account.equals(parts[ACCOUNT_PART])) {
                        throw new IllegalStateException("A user can belong to only one account.");
                    }
                    account = parts[ACCOUNT_PART];
                }
            }
            String userId = userNode.get("id").asText();
            String email = userNode.get("userName").asText();
            return new PeriscopeUser(userId, email, account);
        } catch (IOException e) {
            throw new UserDetailsUnavailableException("User details cannot be retrieved from identity server.", e);
        }
    }

    private String getAuthorizationHeader(String clientId, String clientSecret) {
        String creds = String.format("%s:%s", clientId, clientSecret);
        try {
            return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not convert String");
        }
    }
}
