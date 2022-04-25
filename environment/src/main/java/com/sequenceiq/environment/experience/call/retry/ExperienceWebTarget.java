package com.sequenceiq.environment.experience.call.retry;

import static com.sequenceiq.environment.experience.call.retry.RetryConstants.BACKOFF_DELAY_IN_MS;
import static com.sequenceiq.environment.experience.call.retry.RetryConstants.MAX_ATTEMPTS;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExperienceWebTarget {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceWebTarget.class);

    public Response get(Builder call) {
        LOGGER.info("GET called [{}] with the maximum amount of retry of {} and with the backoff delay of {}", call.toString(), MAX_ATTEMPTS,
                BACKOFF_DELAY_IN_MS);
        return call.get();
    }

    public Response delete(Builder call) {
        LOGGER.info("DELETE called [{}] with the maximum amount of retry of {} and with the backoff delay of {}", call.toString(), MAX_ATTEMPTS,
                BACKOFF_DELAY_IN_MS);
        return call.delete();
    }

}
