package com.sequenceiq.flow.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;

public abstract class EventParameterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventParameterFactory.class);

    private final CrnUserDetailsService crnUserDetailsService;

    public EventParameterFactory(CrnUserDetailsService crnUserDetailsService) {
        this.crnUserDetailsService = crnUserDetailsService;
    }

    public Map<String, Object> createEventParameters(Long resourceId) {
        Map<String, Object> eventParameters = new HashMap<>();
        getUserCrn(resourceId).ifPresent(userCrn -> eventParameters.put(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn));
        return eventParameters;
    }

    private Optional<String> getUserCrn(Long resourceId) {
        Optional<String> userCrn;
        try {
            userCrn = Optional.ofNullable(ThreadBasedUserCrnProvider.getUserCrn())
                    .or(() -> {
                        LOGGER.debug("User crn is empty, trying to get it with getUserCrnByResourceId");
                        return getUserCrnByResourceId(resourceId);
                    });
        } catch (Exception e) {
            LOGGER.warn("Failed to get user crn from ThreadBasedUserCrnProvider, trying to get it with getUserCrnByResourceId", e);
            userCrn = getUserCrnByResourceId(resourceId);
        }
        userCrn = userCrn.filter(this::isUserExistsInUms);
        if (userCrn.isEmpty()) {
            LOGGER.warn("User crn is empty or does not exist in UMS");
        }
        return userCrn;
    }

    protected abstract Optional<String> getUserCrnByResourceId(Long resourceId);

    private boolean isUserExistsInUms(String userCrn) {
        try {
            crnUserDetailsService.getUmsUser(userCrn);
            return true;
        } catch (Exception e) {
            LOGGER.warn("User crn {} does not exist in UMS", userCrn, e);
            return false;
        }
    }

}
