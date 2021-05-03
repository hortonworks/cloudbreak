package com.sequenceiq.cloudbreak.cloud.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.Retry;

@Component
public class CloudExceptionConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudExceptionConverter.class);

    public CloudConnectorException convertToCloudConnectorException(Throwable e, String actionDescription) {
        LOGGER.warn(String.format("%s failed, %s happened:", actionDescription, e != null ? e.getClass().getName() : null), e);
        Throwable eEffective = unwrapActionFailedException(e);
        CloudConnectorException result;
        if (eEffective instanceof CloudConnectorException) {
            result = (CloudConnectorException) eEffective;
        } else {
            result = new CloudConnectorException(String.format("%s failed: %s", actionDescription, eEffective != null ? eEffective.getMessage() : null),
                    eEffective);
        }
        return result;
    }

    private Throwable unwrapActionFailedException(Throwable e) {
        return e instanceof Retry.ActionFailedException && e.getCause() != null ? e.getCause() : e;
    }

}
