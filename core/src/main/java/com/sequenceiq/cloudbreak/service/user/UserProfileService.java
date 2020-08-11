package com.sequenceiq.cloudbreak.service.user;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    private final Object lockObject = new Object();

    @Inject
    private UserProfileRepository userProfileRepository;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    public UserProfile getOrCreateForLoggedInUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return getOrCreate(user);
    }

    public UserProfile getOrCreate(User user) {
        UserProfile userProfile = getSilently(user).orElse(null);
        if (userProfile == null) {
            userProfile = creteUserProfile(user);
        } else if (userProfile.getUserName() == null && user.getUserName() != null) {
            userProfile = updateUserProfileUserName(user);
        } else if (userProfile.getUser() == null) {
            userProfile = updateUserProfileUser(user);
        }
        return userProfile;
    }

    public UserProfile save(UserProfile userProfile) {
        synchronized (lockObject) {
            return userProfileRepository.save(userProfile);
        }
    }

    private Optional<UserProfile> getSilently(User user) {
        return userProfileRepository.findOneByUser(user.getId());
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

    private UserProfile creteUserProfile(User user) {
        UserProfile userProfile;
        LOGGER.debug("UserProfile is null for User {} ({})", user.getUserName(), user.getId());
        synchronized (lockObject) {
            userProfile = getSilently(user).orElse(null);
            if (userProfile == null) {
                LOGGER.debug("UserProfile is still null for User {} ({}). Creating new UserProfile record.", user.getUserName(), user.getId());
                userProfile = new UserProfile();
                userProfile.setUserName(user.getUserName());
                addUiProperties(userProfile);
                userProfile.setUser(user);
                userProfile = saveUserProfile(userProfile);
                LOGGER.debug("UserProfile record created. {} ({})", userProfile.getUserName(), userProfile.getId());
            }
        }
        return userProfile;
    }

    private UserProfile updateUserProfileUserName(User user) {
        UserProfile userProfile;
        LOGGER.debug("UserProfile had empty UserName for User {} ({})", user.getUserName(), user.getId());
        synchronized (lockObject) {
            userProfile = getSilently(user).orElseThrow(() ->
                    new IllegalStateException(String.format("UserProfile became NULL for user %s (%d)", user.getUserName(), user.getId())));
            if (userProfile.getUserName() == null && user.getUserName() != null) {
                LOGGER.debug("UserProfile still had empty UserName for User {} ({}). Setting.", user.getUserName(), user.getId());
                userProfile.setUserName(user.getUserName());
                userProfile = userProfileRepository.save(userProfile);
                LOGGER.debug("UserProfile record saved. {} ({})", userProfile.getUserName(), userProfile.getId());
            }
        }
        return userProfile;
    }

    private UserProfile saveUserProfile(UserProfile userProfile) {
        try {
            return userProfileRepository.save(userProfile);
        } catch (RuntimeException ex) {
            String userName = userProfile.getUser().getUserName();
            LOGGER.warn(String.format("Exception has thrown during saving the userProfile for %s user to the DB." +
                    " It might be saved earlier, trying to get it from the DB.", userName), ex);
            return userProfileRepository.findOneByUser(userProfile.getUser().getId()).orElseThrow(
                    () -> new AccessDeniedException(String.format("UserProfile cannot be created for %s user", userName), ex));
        }
    }

    private UserProfile updateUserProfileUser(User user) {
        UserProfile userProfile;
        LOGGER.debug("UserProfile had no User Id set for User {} ({})", user.getUserName(), user.getId());
        synchronized (lockObject) {
            userProfile = getSilently(user).orElseThrow(() ->
                    new IllegalStateException(String.format("UserProfile became NULL for user %s (%d)", user.getUserName(), user.getId())));
            if (userProfile.getUser() == null) {
                LOGGER.debug("UserProfile still had no User Id set for User {} ({}). Setting.", user.getUserName(), user.getId());
                userProfile.setUser(user);
                userProfile = userProfileRepository.save(userProfile);
                LOGGER.debug("UserProfile record saved. {} ({})", userProfile.getUserName(), userProfile.getId());
            }
        }
        return userProfile;
    }
}
