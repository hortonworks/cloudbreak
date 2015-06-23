package com.sequenceiq.cloudbreak.service.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Service
public class RemoteUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteUserDetailsService.class);

    private static final int ACCOUNT_PART = 2;
    private static final int ROLE_PART = 2;

    @Value("${cb.client.id}")
    private String clientId;

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Value("${cb.identity.server.url}")
    private String identityServerUrl;

    @Inject
    @Qualifier("restTemplate")
    private RestOperations restTemplate;

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

    @Override
    @Cacheable(value = "userCache", key = "#filterValue")
    public CbUser getDetails(String filterValue, UserFilterField filterField) {

        HttpHeaders tokenRequestHeaders = new HttpHeaders();
        tokenRequestHeaders.set("Authorization", getAuthorizationHeader(clientId, clientSecret));

        Map<String, String> tokenResponse = restTemplate.exchange(
                identityServerUrl + "/oauth/token?grant_type=client_credentials",
                HttpMethod.POST,
                new HttpEntity<>(tokenRequestHeaders),
                Map.class).getBody();

        HttpHeaders scimRequestHeaders = new HttpHeaders();
        scimRequestHeaders.set("Authorization", "Bearer " + tokenResponse.get("access_token"));

        String scimResponse = null;

        switch (filterField) {
            case USERNAME:
                scimResponse = restTemplate.exchange(
                        identityServerUrl + "/Users/" + "?filter=userName eq \"" + filterValue + "\"",
                        HttpMethod.GET,
                        new HttpEntity<>(scimRequestHeaders),
                        String.class).getBody();
                break;
            case USERID:
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
            List<CbUserRole> roles = new ArrayList<>();
            String account = null;
            JsonNode userNode = root;
            if (UserFilterField.USERNAME.equals(filterField)) {
                userNode = root.get("resources").get(0);
            }
            for (Iterator<JsonNode> iterator = userNode.get("groups").getElements(); iterator.hasNext();) {
                JsonNode node = iterator.next();
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
            String givenName = userNode.get("name").get("givenName").asText();
            String familyName = userNode.get("name").get("familyName").asText();
            return new CbUser(userId, email, account, roles, givenName, familyName);
        } catch (IOException e) {
            throw new UserDetailsUnavailableException("User details cannot be retrieved from identity server.", e);
        }
    }

    @Override
    @CacheEvict(value = "userCache", key = "#filterValue")
    public void evictUserDetails(String updatedUserId, String filterValue) {
        LOGGER.info("Remove userid: {} / username: {} from user cache", updatedUserId, filterValue);
    }

    @Override
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
}
