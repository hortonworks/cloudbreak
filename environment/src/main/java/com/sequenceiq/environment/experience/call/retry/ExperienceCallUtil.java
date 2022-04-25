package com.sequenceiq.environment.experience.call.retry;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static com.sequenceiq.environment.experience.call.retry.RetryConstants.BACKOFF_DELAY_IN_MS;
import static com.sequenceiq.environment.experience.call.retry.RetryConstants.MAX_ATTEMPTS;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Callable;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ResponseReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperienceCallUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceCallUtil.class);

    private ExperienceCallUtil() {
    }

    public static <T> Optional<T> fetchResultWithRetry(ResponseReader responseReader, URI path, Callable<Response> toCall, Class<T> resolveResponse,
            String messageUponSingleFailure) {
        throwIfNull(responseReader, () -> new IllegalArgumentException(ResponseReader.class.getName() + " instance should not be null!"));
        throwIfNull(toCall, () -> new IllegalArgumentException("No " + Callable.class.getSimpleName() + " was given, therefore no action can be done!"));
        byte retryCount = 1;
        do {
            LOGGER.debug("Trying to get response from experience on path: " + path.getPath() + " for the #" + retryCount + " time");
            try (Response response = executeCall(path, toCall)) {
                LOGGER.debug("Response has arrived from experience, its about to get read by " + responseReader.getClass().getSimpleName());
                Optional<T> resolvedResponse = responseReader.read(path.toString(), response, resolveResponse);
                if (resolvedResponse.isPresent()) {
                    LOGGER.debug("The following response has resolved and returning it for further inspection: " +  resolvedResponse.get());
                    return resolvedResponse;
                }
            } catch (RuntimeException re) {
                LOGGER.warn(messageUponSingleFailure, re);
            }
            LOGGER.debug("Resolving response from experience call was not successful on the #" + retryCount + " attempt");
            retryCount++;
            try {
                LOGGER.debug("Retrying the call of the given experience is going to be delayed by " + BACKOFF_DELAY_IN_MS + "ms");
                Thread.sleep(BACKOFF_DELAY_IN_MS);
            } catch (InterruptedException e) {
                LOGGER.info("Waiting between experience call retry has been interrupted due to: ", e);
            }
        } while (retryCount <= MAX_ATTEMPTS);
        LOGGER.debug("We were unable to resolve a proper response in + " + retryCount + " attempts from the experience (on path: " + path.getPath() +
                ") so empty result is going to be returned.");
        return Optional.empty();
    }

    private static Response executeCall(URI path, Callable<Response> toCall) {
        LOGGER.debug("About to connect to Kubernetes Experience on path: {}", path);
        try {
            return toCall.call();
        } catch (Exception re) {
            LOGGER.warn("Kubernetes Experience http call execution has failed due to:", re);
            throw new ExperienceOperationFailedException(re);
        }
    }

}
