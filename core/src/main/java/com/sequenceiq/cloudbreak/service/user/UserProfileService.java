package com.sequenceiq.cloudbreak.service.user;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    @Inject
    private UserProfileRepository userProfileRepository;

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
            synchronized (UserProfileService.class) {
                userProfile = getSilently(user).orElse(null);
                if (userProfile == null) {
                    userProfile = new UserProfile();
                    userProfile.setUserName(user.getUserName());
                    addUiProperties(userProfile);
                    userProfile.setUser(user);
                    userProfile = userProfileRepository.save(userProfile);
                }
            }
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
        return userProfileRepository.findOneByUser(user.getId());
    }

    public UserProfile save(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public Set<UserProfile> findByImageCatalogId(Long catalogId) {
        return userProfileRepository.findOneByImageCatalogName(catalogId);
    }

    private void addUiProperties(UserProfile userProfile) {
        try {
            userProfile.setUiProperties(new Json(new HashMap<>()).getValue());
        } catch (IllegalArgumentException ignored) {
            userProfile.setUiProperties(null);
        }
    }
}
