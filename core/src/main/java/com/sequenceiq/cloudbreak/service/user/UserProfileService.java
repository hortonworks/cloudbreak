package com.sequenceiq.cloudbreak.service.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.UserProfileV4Request;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

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
    private SecretService secretService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    public UserProfile getOrCreateForLoggedInUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return getOrCreate(user);
    }

    public UserProfile getOrCreate(User user) {
        UserProfile userProfile = getSilently(user).orElse(null);
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserName(user.getUserName());
            addUiProperties(userProfile);
            userProfile.setUser(user);
            userProfile.setDefaultCredentials(Collections.emptySet());
            userProfile = userProfileRepository.save(userProfile);
        } else if (userProfile.getUserName() == null && user.getUserName() != null) {
            userProfile.setUserName(user.getUserName());
            userProfile = userProfileRepository.save(userProfile);
        } else if (userProfile.getUser() == null) {
            userProfile.setUser(user);
            userProfile = userProfileRepository.save(userProfile);
        }
        return userProfile;
    }

    private Optional<UserProfile> getSilently(User user) {
        try {
            return userProfileRepository.findOneByUser(user.getId());
        } catch (AccessDeniedException ignore) {
            return Optional.empty();
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

    private void addUiProperties(UserProfile userProfile) {
        try {
            userProfile.setUiProperties(new Json(new HashMap<>()).getValue());
        } catch (JsonProcessingException ignored) {
            userProfile.setUiProperties(null);
        }
    }

    public UserProfile putForLoggedInUser(UserProfileV4Request request, Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(workspaceId, user);
        return put(request, user, workspace);
    }

    public UserProfile put(UserProfileV4Request request, User user, Workspace workspace) {
        UserProfile userProfile = getOrCreate(user);
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
        String oldVaultSecret = userProfile.getUiPropertiesSecret();
        String uiPropertiesFromVault = userProfile.getUiProperties();
        Map<String, Object> map = new Json(uiPropertiesFromVault).getMap();
        try {
            userProfile.setUiProperties(new Json(map).getValue());
        } catch (JsonProcessingException ignored) {
            throw new BadRequestException("The modification of the ui properties was unsuccesfull.");
        }
        UserProfile newUserProfile = userProfileRepository.save(userProfile);
        secretService.delete(oldVaultSecret);
        return newUserProfile;
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
