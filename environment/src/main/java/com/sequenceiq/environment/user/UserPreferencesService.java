package com.sequenceiq.environment.user;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Service
public class UserPreferencesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository userPreferencesRepository;

    public UserPreferencesService(UserPreferencesRepository userPreferencesRepository) {
        this.userPreferencesRepository = userPreferencesRepository;
    }

    public String getExternalIdForCurrentUser() {
        return getExternalId(ThreadBasedUserCrnProvider.getUserCrn());
    }

    public String getExternalId(String userCrn) {
        LOGGER.debug("Get or create user preferences with external id for user '{}'", userCrn);
        Optional<UserPreferences> userPreferencesOptional = userPreferencesRepository.findByUserCrn(userCrn);
        UserPreferences userPreferences;
        if (userPreferencesOptional.isPresent()) {
            userPreferences = userPreferencesOptional.get();
            if (StringUtils.isEmpty(userPreferences.getExternalId())) {
                LOGGER.debug("External id exist for current user with crn '{}'", userCrn);
                userPreferences.setExternalId(generateExternalId());
            }
        } else {
            LOGGER.debug("User preferences does not exist, creating it with crn '{}'", userCrn);
            userPreferences = new UserPreferences(generateExternalId(), userCrn);
        }
        return userPreferencesRepository.save(userPreferences).getExternalId();
    }

    private String generateExternalId() {
        return UUID.randomUUID().toString();
    }
}
