package com.sequenceiq.thunderhead.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;

@Component
public class UserStateStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserStateStoreService.class);

    private final Map<String, UserState> userStates = new HashMap<>();

    public void store(String environmentCrn, UserState userState) {
        LOGGER.info("Store user state for environment: {}", environmentCrn);
        userStates.put(environmentCrn, userState);
    }

    public void remove(String environmentCrn) {
        LOGGER.info("Remove user state from store for environment: {}", environmentCrn);
        userStates.remove(environmentCrn);
    }

    public UserState fetch(String environmentCrn) {
        LOGGER.info("Fetch user state from store for environment: {}", environmentCrn);
        return userStates.get(environmentCrn);
    }
}
