package com.sequenceiq.environment.user;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;

@Service
public class UserPreferencesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository userPreferencesRepository;

    private final AuthenticatedUserService authenticatedUserService;

    public UserPreferencesService(UserPreferencesRepository userPreferencesRepository, AuthenticatedUserService authenticatedUserService) {
        this.userPreferencesRepository = userPreferencesRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public String getExternalIdForCurrentUser() {
        return getExternalId(authenticatedUserService.getCbUser().getUserCrn());
    }

    public String getExternalId(String userCrn) {
        LOGGER.debug("Get or create user preferences with external id for user '{}'", userCrn);
        Optional<UserPreferences> userPreferencesOptional = userPreferencesRepository.findByUserCrn(userCrn);
        UserPreferences userPreferences;
        if (userPreferencesOptional.isPresent()) {
            userPreferences = userPreferencesOptional.get();
            if (StringUtils.isEmpty(userPreferences.getExternalId())) {
                userPreferences.setExternalId(generateExternalId());
            }
        } else {
            userPreferences = new UserPreferences(generateExternalId(), userCrn);
        }
        return userPreferencesRepository.save(userPreferences).getExternalId();
    }

    private String generateExternalId() {
        return UUID.randomUUID().toString();
    }
}
