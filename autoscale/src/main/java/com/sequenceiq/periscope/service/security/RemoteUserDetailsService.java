package com.sequenceiq.periscope.service.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    private static final int ACCOUNT_PART = 2;

    @Autowired
    private ClientBuilder clientBuilder;

    @Autowired
    @Qualifier("identityServerUrl")
    private String identityServerUrl;

    @Value("${periscope.client.id}")
    private String clientId;

    @Value("${periscope.client.secret}")
    private String clientSecret;

    @Override
    @Cacheable("userCache")
    @SuppressWarnings("unchecked")
    public PeriscopeUser getDetails(String filterValue, UserFilterField filterField) {
        Client client = clientBuilder.build();
        WebTarget target = client.target(identityServerUrl).path("/oauth/token").queryParam("grant_type", "client_credentials");
        Map<String, String> tokenResponse = target.request(MediaType.APPLICATION_JSON).header("Authorization", getAuthorizationHeader(clientId, clientSecret))
                .post(Entity.json(null), Map.class);
        client.close();

        Client scimClient = clientBuilder.build();
        WebTarget scimTarget = scimClient.target(identityServerUrl).path("Users");
        switch (filterField) {
            case USERNAME:
                scimTarget.queryParam("filter", "userName eq \"" + filterValue + "\"");
                break;
            case USER_ID:
                scimTarget.path(filterValue);
                break;
            default:
                throw new UserDetailsUnavailableException("User details cannot be retrieved.");
        }
        String scimResponse = scimTarget.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + tokenResponse.get("access_token"))
                .get(String.class);
        scimClient.close();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(scimResponse);
            JsonNode userNode = root;
            if (UserFilterField.USERNAME.equals(filterField)) {
                userNode = root.get("resources").get(0);
            }
            String account = null;
            for (Iterator<JsonNode> iterator = userNode.get("groups").getElements(); iterator.hasNext();) {
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
