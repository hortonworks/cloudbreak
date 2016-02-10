package com.sequenceiq.periscope.service.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
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

import com.sequenceiq.cloudbreak.client.AccessToken;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    private static final int ACCOUNT_PART = 2;

    @Autowired
    private Client restClient;

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    @Qualifier("identityServerUrl")
    private String identityServerUrl;

    @Value("${periscope.client.secret}")
    private String clientSecret;

    private WebTarget identityWebTarget;

    @PostConstruct
    public void init() {
        identityWebTarget = restClient.target(identityServerUrl).path("Users");
    }

    @Override
    @Cacheable("userCache")
    @SuppressWarnings("unchecked")
    public PeriscopeUser getDetails(String filterValue, UserFilterField filterField) {
        WebTarget target;
        switch (filterField) {
            case USERNAME:
                target = identityWebTarget.queryParam("filter", "userName eq \"" + filterValue + "\"");
                break;
            case USER_ID:
                target = identityWebTarget.path(filterValue);
                break;
            default:
                throw new UserDetailsUnavailableException("User details cannot be retrieved.");
        }
        AccessToken accessToken = identityClient.getToken(clientSecret);
        String scimResponse = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + accessToken.getToken()).get(String.class);
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
