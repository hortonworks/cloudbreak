package com.sequenceiq.environment.environment.poller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update vs delete discriminator for {@link UserDefinedTagsPollerSupport}. Logging and per-operation conflict policy
 * live here; see that class for the full behavior matrix.
 */
enum UserDefinedTagsModificationType {
    UPDATE,
    DELETE;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDefinedTagsModificationType.class);

    void logAttempt(int resourceCount, String resourceTypeLabel, Long envId) {
        if (this == UPDATE) {
            LOGGER.info("Attempting to update user defined tags on {} {} for environment with ID {}",
                    resourceCount, resourceTypeLabel, envId);
        } else {
            LOGGER.info("Attempting to delete user defined tag keys on {} {} for environment with ID {}",
                    resourceCount, resourceTypeLabel, envId);
        }
    }

    void logTrigger(String remoteServiceName, String resourceLabel, String resourceCrn) {
        if (this == UPDATE) {
            LOGGER.info("Calling {} to update user defined tags for {} {}", remoteServiceName, resourceLabel, resourceCrn);
        } else {
            LOGGER.info("Calling {} to delete user defined tag keys for {} {}", remoteServiceName, resourceLabel, resourceCrn);
        }
    }

    void logFlowConflictRetry(String resourceCrn, String resourceLabelForMessage) {
        if (this == UPDATE) {
            LOGGER.info("Unable to start user defined tags update for {}. {} has flow running already. Retrying.",
                    resourceCrn, resourceLabelForMessage);
        } else {
            LOGGER.info("Unable to start user defined tag deletion for {}. {} has flow running already. Retrying.",
                    resourceCrn, resourceLabelForMessage);
        }
    }

    void logUnexpectedFailure(String remoteServiceName, String errorMessage) {
        if (this == UPDATE) {
            LOGGER.warn("Failure asking {} for user defined tags update, error message is: {}",
                    remoteServiceName, errorMessage);
        } else {
            LOGGER.warn("Failure asking {} for user defined tag deletion, error message is: {}",
                    remoteServiceName, errorMessage);
        }
    }

    String resourceLabelForConflictMessage(String resourceLabel) {
        if (resourceLabel.isEmpty()) {
            return resourceLabel;
        }
        return Character.toUpperCase(resourceLabel.charAt(0)) + resourceLabel.substring(1);
    }
}
