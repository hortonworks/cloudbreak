package com.sequenceiq.cloudbreak.service.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    @Inject
    private UserProfileRepository userProfileRepository;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private UserService userService;

    @Inject
    private SecretService secretService;

    public UserProfile getOrCreate(String account, String owner, User user) {
        return getOrCreate(account, owner, null, user);
    }

    public UserProfile getOrCreate(String account, String owner, String userName, User user) {
        UserProfile userProfile = getSilently(account, owner);
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setAccount(account);
            userProfile.setOwner(owner);
            userProfile.setUserName(userName);
            addUiProperties(userProfile);
            userProfile.setUser(user);
            userProfile.setDefaultCredentials(Collections.emptySet());
            userProfile = userProfileRepository.save(userProfile);
        } else if (userProfile.getUserName() == null && userName != null) {
            userProfile.setUserName(userName);
            userProfile = userProfileRepository.save(userProfile);
        } else if (userProfile.getUser() == null) {
            userProfile.setUser(user);
            userProfile = userProfileRepository.save(userProfile);
        }
        return userProfile;
    }

    private UserProfile getSilently(String account, String owner) {
        try {
            return userProfileRepository.findOneByOwnerAndAccount(account, owner);
        } catch (AccessDeniedException ignore) {
            return null;
        }
    }

    public UserProfile save(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public Set<UserProfile> findOneByCredentialId(Long credentialId) {
        return userProfileRepository.findOneByCredentialId(credentialId);
    }

    public Set<UserProfile> findByImageCatalogId(Long catalogId) {
        return userProfileRepository.findOneByImageCatalogName(catalogId);
    }

    public UserProfile findByUser(Long userId) {
        return userProfileRepository.findOneByUser(userId);
    }

    private void addUiProperties(UserProfile userProfile) {
        try {
            userProfile.setUiProperties(new Json(new HashMap<>()).getValue());
        } catch (JsonProcessingException ignored) {
            userProfile.setUiProperties(null);
        }
    }

    public void put(UserProfileRequest request, CloudbreakUser cloudbreakUser, User user, Workspace workspace) {
        UserProfile userProfile = getOrCreate(cloudbreakUser.getAccount(), cloudbreakUser.getUserId(), cloudbreakUser.getUsername(), user);
        if (request.getCredentialId() != null) {
            Credential credential = credentialService.get(request.getCredentialId(), workspace);
            storeDefaultCredential(userProfile, credential, workspace);
        } else if (request.getCredentialName() != null) {
            Credential credential = credentialService.getByNameForWorkspace(request.getCredentialName(), workspace);
            storeDefaultCredential(userProfile, credential, workspace);
        }
        if (request.getImageCatalogName() != null) {
            Long workspaceId = workspace.getId();
            ImageCatalog imageCatalog = imageCatalogService.get(workspaceId, request.getImageCatalogName());
            userProfile.setImageCatalog(imageCatalog);
        }
        String oldVaultPath = userProfile.getUiProperties();
        String uiPropertiesFromVault = secretService.get(userProfile.getUiProperties());
        Map<String, Object> map = new Json(uiPropertiesFromVault).getMap();
        for (Entry<String, Object> uiStringObjectEntry : request.getUiProperties().entrySet()) {
            map.put(uiStringObjectEntry.getKey(), uiStringObjectEntry.getValue());
        }
        try {
            userProfile.setUiProperties(new Json(map).getValue());
        } catch (JsonProcessingException ignored) {
            throw new BadRequestException("The modification of the ui properties was unsuccesfull.");
        }
        userProfileRepository.save(userProfile);
        secretService.delete(oldVaultPath);
    }

    private void storeDefaultCredential(UserProfile userProfile, Credential credential, Workspace workspace) {
        if (userProfile.getDefaultCredentials() == null) {
            userProfile.setDefaultCredentials(new HashSet<>());
        }
        Set<Credential> removableCredentials = userProfile.getDefaultCredentials().stream()
                .filter(defaultCredential -> defaultCredential.getWorkspace().getId().equals(workspace.getId()))
                .collect(Collectors.toSet());
        userProfile.getDefaultCredentials().removeAll(removableCredentials);
        userProfile.getDefaultCredentials().add(credential);
    }
}
