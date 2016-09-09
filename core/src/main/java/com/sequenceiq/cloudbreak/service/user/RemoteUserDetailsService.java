package com.sequenceiq.cloudbreak.service.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.client.AccessToken;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteUserDetailsService.class);
    private static final int ACCOUNT_PART = 2;
    private static final int ROLE_PART = 2;
    private static final String UAA_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Inject
    @Named("identityServerUrl")
    private String identityServerUrl;

    @Inject
    private Client restClient;

    @Inject
    private IdentityClient identityClient;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private NetworkRepository networkRepository;

    private WebTarget identityWebTarget;

    @PostConstruct
    public void init() {
        identityWebTarget = restClient.target(identityServerUrl).path("Users");
    }

    @Override
    @Cacheable(value = "userCache", key = "#filterValue")
    public CbUser getDetails(String filterValue, UserFilterField filterField) {
        WebTarget target;
        switch (filterField) {
            case USERNAME:
                target = identityWebTarget.queryParam("filter", "userName eq \"" + filterValue + "\"");
                break;
            case USERID:
                target = identityWebTarget.path(filterValue);
                break;
            default:
                throw new UserDetailsUnavailableException("User details cannot be retrieved.");
        }
        AccessToken accessToken = identityClient.getToken(clientSecret);
        String scimResponse = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + accessToken.getToken()).get(String.class);
        try {
            JsonNode root = JsonUtil.readTree(scimResponse);
            List<CbUserRole> roles = new ArrayList<>();
            String account = null;
            JsonNode userNode = root;
            if (UserFilterField.USERNAME.equals(filterField)) {
                userNode = root.get("resources").get(0);
            }
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
                    roles.add(CbUserRole.fromString(parts[ROLE_PART]));
                }
            }
            String userId = userNode.get("id").asText();
            String email = userNode.get("userName").asText();
            String givenName = getGivenName(userNode);
            String familyName = getFamilyName(userNode);
            String dateOfCreation = userNode.get("meta").get("created").asText();
            Date created = parseUserCreated(dateOfCreation);
            return new CbUser(userId, email, account, roles, givenName, familyName, created);
        } catch (IOException e) {
            throw new UserDetailsUnavailableException("User details cannot be retrieved from identity server.", e);
        }
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

    @Override
    @CacheEvict(value = "userCache", key = "#filterValue")
    public void evictUserDetails(String updatedUserId, String filterValue) {
        LOGGER.info("Remove userid: {} / username: {} from user cache", updatedUserId, filterValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasResources(CbUser admin, String userId) {
        CbUser user = getDetails(userId, UserFilterField.USERID);
        LOGGER.info("{} / {} checks resources of {}", admin.getUserId(), admin.getUsername(), userId);
        String errorMessage = null;
        if (!admin.getRoles().contains(CbUserRole.ADMIN)) {
            errorMessage = "Forbidden: user (%s) is not authorized for this operation on %s";
        }
        if (!admin.getAccount().equals(user.getAccount())) {
            errorMessage = "Forbidden: admin (%s) and user (%s) are not under the same account.";
        }
        if (!Strings.isNullOrEmpty(errorMessage)) {
            throw new AccessDeniedException(String.format(errorMessage, admin.getUsername(), user.getUsername()));
        }
        Set<Template> templates = templateRepository.findForUser(user.getUserId());
        Set<Credential> credentials = credentialRepository.findForUser(user.getUserId());
        Set<Blueprint> blueprints = blueprintRepository.findForUser(user.getUserId());
        Set<Network> networks = networkRepository.findForUser(user.getUserId());
        Set<Stack> stacks = stackRepository.findForUser(user.getUserId());
        return !(stacks.isEmpty() && templates.isEmpty() && credentials.isEmpty()
                && blueprints.isEmpty() && networks.isEmpty());
    }

    private String getAuthorizationHeader(String clientId, String clientSecret) {
        String creds = String.format("%s:%s", clientId, clientSecret);
        try {
            return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not convert String");
        }
    }

    private Date parseUserCreated(String dateOfCreation) {
        try {
            SimpleDateFormat uaaDateFormat = new SimpleDateFormat(UAA_DATE_PATTERN);
            return uaaDateFormat.parse(dateOfCreation);
        } catch (ParseException e) {
            throw new UserDetailsUnavailableException("User details cannot be retrieved, becuase creation date of user cannot be parsed.", e);
        }
    }
}
