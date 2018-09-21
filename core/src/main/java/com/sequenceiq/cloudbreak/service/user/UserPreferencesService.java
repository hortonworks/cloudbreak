package com.sequenceiq.cloudbreak.service.user;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserPreferences;
import com.sequenceiq.cloudbreak.repository.workspace.UserPreferencesRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;

@Service
public class UserPreferencesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferencesService.class);

    @Inject
    private UserPreferencesRepository userPreferencesRepository;

    @Inject
    private UserService userService;

    @Inject
    private TransactionService transactionService;

    public Optional<UserPreferences> getByUser(User user) {
        LOGGER.debug("Get user preferences for user '{}'", user.getUserId());
        return userPreferencesRepository.findByUser(user);
    }

    public Optional<UserPreferences> getByUserId(String userId) {
        return getByUser(userService.getByUserId(userId));
    }

    public UserPreferences getWithExternalId(User user) {
        LOGGER.info("Get or create user preferences with external id for user '{}'", user.getUserId());
        UserPreferences result;
        Optional<UserPreferences> userOptional = getByUser(user);
        if (userOptional.isPresent()) {
            result = userOptional.get();
            if (StringUtils.isEmpty(result.getExternalId())) {
                result.setExternalId(generateExternalId());
                result = update(result);
            }
            return result;
        }
        throw new NotFoundException(String.format("User preferences could not be found for user that has id '%s'.", user.getUserId()));
    }

    private String generateExternalId() {
        return UUID.randomUUID().toString();
    }

    private UserPreferences update(UserPreferences userPreferences) {
        User user = userPreferences.getUser();
        LOGGER.info("Update user preferences for user '{}'", user.getUserId());
        try {
            return transactionService.required(() -> userPreferencesRepository.save(userPreferences));
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.warn("UserPreferences could not be updated. ", e);
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }
}
