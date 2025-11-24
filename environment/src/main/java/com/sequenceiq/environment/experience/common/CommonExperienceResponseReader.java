package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static com.sequenceiq.environment.experience.ResponseReaderUtility.logInputResponseContentIfPossible;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.util.Optional;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.ResponseReader;

@Component
public class CommonExperienceResponseReader implements ResponseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperienceResponseReader.class);

    @Override
    public <T> Optional<T> read(String target, Response response, Class<T> expectedType) {
        throwIfNull(response, () -> new IllegalArgumentException("Response should not be null!"));
        T experienceCallResponse = null;
        LOGGER.debug("Going to read response from experience call");
        logInputResponseContentIfPossible(LOGGER, response, "Common experience response: {}");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                experienceCallResponse = response.readEntity(expectedType);
            } catch (IllegalStateException | ProcessingException e) {
                String msg = "Failed to resolve response from experience on path: " + target;
                LOGGER.warn(msg, e);
            }
        } else {
            String status = response.getStatusInfo().getReasonPhrase();
            LOGGER.info("Calling experience ( on path : {} ) was not successful! Status was: {}", target, status);
        }
        return Optional.ofNullable(experienceCallResponse);
    }

}
