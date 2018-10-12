package com.sequenceiq.cloudbreak.common.service.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.client.AccessToken;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsService.class);

    private static final int ACCOUNT_PART = 2;

    private static final String UAA_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final String EMAIL_SEPARATOR = "@";

    private static final long ITEMS_PER_PAGE = 1000L;

    @Inject
    @Named("identityServerUrl")
    private String identityServerUrl;

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cert.validation}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation}")
    private boolean ignorePreValidation;

    @Inject
    private IdentityClient identityClient;

    private WebTarget identityWebTarget;

    @PostConstruct
    public void init() {
        ConfigKey configKey = new ConfigKey(certificateValidation, restDebug, ignorePreValidation);
        identityWebTarget = RestClientUtil.get(configKey).target(identityServerUrl).path("Users");
    }

    public CloudbreakUser getDetails(String username, UserFilterField filterField, String clientSecret) {
        try {
            return getIdentityUser(username, filterField, clientSecret);
        } catch (UserDetailsUnavailableException e) {
            LOGGER.info("{} Assume SSO token", e.getMessage());
            String account = username;
            if (username.contains(EMAIL_SEPARATOR)) {
                account = username.substring(username.indexOf(EMAIL_SEPARATOR) + 1);
            }
            return new CloudbreakUser(username, username, account);
        }
    }

    public List<CloudbreakUser> getAllUsers(String clientSecret) {
        try {
            AccessToken accessToken = identityClient.getToken(clientSecret);
            List<CloudbreakUser> cloudbreakUsers = new ArrayList<>();
            long totalResults = getTotalResults(accessToken);
            for (long startIndex = 1L; startIndex <= totalResults; startIndex += ITEMS_PER_PAGE) {
                WebTarget target = identityWebTarget.queryParam("startIndex", startIndex)
                        .queryParam("count", ITEMS_PER_PAGE)
                        .queryParam("sortBy", "id");
                String result = target.request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken.getToken()).get(String.class);

                JsonNode root = JsonUtil.readTree(result);
                root.get("resources").forEach(resource -> {
                    CloudbreakUser cloudbreakUser = getIdentityUserFromJson(resource);
                    cloudbreakUsers.add(cloudbreakUser);
                });
            }

            return cloudbreakUsers;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long getTotalResults(AccessToken accessToken) {
        try {
            WebTarget target = identityWebTarget.queryParam("startIndex", 1)
                    .queryParam("count", 1)
                    .queryParam("sortBy", "id");
            String result = target.request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken.getToken()).get(String.class);
            JsonNode root = JsonUtil.readTree(result);
            return root.get("totalResults").asLong();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private CloudbreakUser getIdentityUser(String username, UserFilterField filterField, String clientSecret) {
        WebTarget target = getWebTarget(username, filterField);
        AccessToken accessToken = identityClient.getToken(clientSecret);
        String scimResponse = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + accessToken.getToken()).get(String.class);
        try {
            JsonNode root = JsonUtil.readTree(scimResponse);
            JsonNode userNode = root;
            if (UserFilterField.USERNAME.equals(filterField)) {
                userNode = root.get("resources").get(0);
            }
            if (userNode == null) {
                throw new UserDetailsUnavailableException("User details cannot be retrieved from identity server.");
            }
            return getIdentityUserFromJson(userNode);
        } catch (IOException e) {
            throw new UserDetailsUnavailableException("User details cannot be retrieved from identity server.", e);
        }
    }

    private CloudbreakUser getIdentityUserFromJson(JsonNode userNode) {
        String account = null;
        for (JsonNode node : userNode.get("groups")) {
            String group = node.get("display").asText();
            if (group.startsWith("sequenceiq.account")) {
                String[] parts = group.split("\\.");
                if (account != null && !account.equals(parts[ACCOUNT_PART])) {
                    throw new IllegalStateException("A user can belong to only one account.");
                }
                account = parts[ACCOUNT_PART];
            } else if (group.startsWith("sequenceiq.cloudbreak")) {
                String[] parts = group.split("\\.");
            }
        }
        return createIdentityUser(account, userNode);
    }

    private WebTarget getWebTarget(String username, UserFilterField filterField) {
        WebTarget target;
        LOGGER.info("Load user details: {}", username);
        switch (filterField) {
            case USERNAME:
                target = identityWebTarget.queryParam("filter", "userName eq \"" + username + '"');
                break;
            case USERID:
                target = identityWebTarget.path(username);
                break;
            default:
                throw new UserDetailsUnavailableException("User details cannot be retrieved.");
        }

        return target;
    }

    private CloudbreakUser createIdentityUser(String account, JsonNode userNode) {
        String userId = userNode.get("id").asText();
        String email = userNode.get("userName").asText();
        account = StringUtils.isEmpty(account) ? userId : account;
        return new CloudbreakUser(userId, email, account);
    }

    private String getGivenName(JsonNode userNode) {
        if (userNode.get("name") != null) {
            if (userNode.get("name").get("givenName") != null) {
                return userNode.get("name").get("givenName").asText();
            }
        }
        return "";
    }

    private String getFamilyName(JsonNode userNode) {
        if (userNode.get("name") != null) {
            if (userNode.get("name").get("familyName") != null) {
                return userNode.get("name").get("familyName").asText();
            }
        }
        return "";
    }
}
