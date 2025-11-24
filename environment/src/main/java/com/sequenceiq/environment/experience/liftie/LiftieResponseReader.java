package com.sequenceiq.environment.experience.liftie;

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
public class LiftieResponseReader implements ResponseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieResponseReader.class);

    @Override
    public <T> Optional<T> read(String target, Response response, Class<T> expectedType) {
        throwIfNull(response, () -> new IllegalStateException("Response should not be null!"));
        T result = null;
        LOGGER.debug("Going to read response from the Liftie call");
        logInputResponseContentIfPossible(LOGGER, response, "Liftie response: {}");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                result = response.readEntity(expectedType);
                LOGGER.debug("Liftie response has resolved.");
            } catch (IllegalStateException | ProcessingException e) {
                String msg = "Failed to resolve response from Liftie on path \"" + target + "\" due to: " + e.getMessage();
                LOGGER.warn(msg, e);
            }
        } else {
            String status = response.getStatusInfo().getReasonPhrase();
            LOGGER.info("Calling Liftie ( on the following path : {} ) was not successful! Status was: {}", target, status);
        }
        return Optional.ofNullable(result);
    }

}
