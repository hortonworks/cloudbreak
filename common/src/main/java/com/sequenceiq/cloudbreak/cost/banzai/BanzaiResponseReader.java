package com.sequenceiq.cloudbreak.cost.banzai;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.util.Optional;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BanzaiResponseReader implements BanzaiBaseResponseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BanzaiResponseReader.class);

    @Override
    public <T> Optional<T> read(String target, Response response, Class<T> expectedType) {
        throwIfNull(response, () -> new IllegalStateException("Response should not be null!"));
        T result = null;
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
