package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ec2.model.Ec2Exception;

@Component
public class AwsMethodExecutor {

    private static final Logger LOGGER = getLogger(AwsMethodExecutor.class);

    public <T> void execute(Supplier<T> awsMethod) {
        execute(awsMethod, null);
    }

    public <T> T execute(Supplier<T> awsMethod, T def) {
        T ret;
        try {
            LOGGER.debug("Fetch data from AWS");
            ret = awsMethod.get();
            LOGGER.debug("Data fetched: {}", ret);
        } catch (Ec2Exception e) {
            if (e.awsErrorDetails().errorCode().contains("NotFound")) {
                LOGGER.info("Aws resource does not found: {}", e.getMessage());
                ret = def;
            } else {
                LOGGER.error("Cannot execute aws method: {}", e.getMessage(), e);
                throw e;
            }
        }
        return ret;
    }
}
