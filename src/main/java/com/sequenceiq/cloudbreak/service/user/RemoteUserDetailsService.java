package com.sequenceiq.cloudbreak.service.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    private static final int ACCOUNT_PART = 2;
    private static final int ROLE_PART = 3;

    @Value("${cb.client.id}")
    private String clientId;

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Value("${cb.identity.server.url}")
    private String identityServerUrl;

    @Override
    @Cacheable("userCache")
    public CbUser getDetails(String username) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders tokenRequestHeaders = new HttpHeaders();
        tokenRequestHeaders.set("Authorization", getAuthorizationHeader(clientId, clientSecret));

        Map<String, String> tokenResponse = restTemplate.exchange(
                identityServerUrl + "/oauth/token?grant_type=client_credentials",
                HttpMethod.POST,
                new HttpEntity<>(tokenRequestHeaders),
                Map.class).getBody();

        HttpHeaders scimRequestHeaders = new HttpHeaders();
        scimRequestHeaders.set("Authorization", "Bearer " + tokenResponse.get("access_token"));

        String scimResponse = restTemplate.exchange(
                identityServerUrl + "/Users/" + "?filter=userName eq \"" + username + "\"",
                HttpMethod.GET,
                new HttpEntity<>(scimRequestHeaders),
                String.class).getBody();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(scimResponse);
            List<CbUserRole> roles = new ArrayList<>();
            String account = null;
            for (Iterator<JsonNode> iterator = root.get("resources").get(0).get("groups").getElements(); iterator.hasNext();) {
                JsonNode node = iterator.next();
                String group = node.get("display").asText();
                if (group.startsWith("cloudbreak.account")) {
                    String[] parts = group.split("\\.");
                    if (account != null && !account.equals(parts[2])) {
                        throw new IllegalStateException("A user can belong to only one account.");
                    }
                    account = parts[ACCOUNT_PART];
                    roles.add(CbUserRole.fromString(parts[ROLE_PART]));
                }
            }
            String givenName = root.get("resources").get(0).get("name").get("givenName").asText();
            String familyName = root.get("resources").get(0).get("name").get("familyName").asText();
            return new CbUser(username, account, roles, givenName, familyName);
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
