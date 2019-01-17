package com.sequenceiq.cloudbreak.service.cluster.ambari;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class AmbariUserHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariUserHandler.class);

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Retryable(value = CloudbreakException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    AmbariClient createAmbariUser(String newUserName, String newPassword, Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        try {
            LOGGER.info("Create user with username: {}", newUserName);
            ambariClient.createUser(newUserName, newPassword, true);
        } catch (Exception e) {
            LOGGER.error("Can not create ambari user", e);
        }
        return checkUser(newUserName, newPassword, stack);
    }

    @Retryable(value = CloudbreakException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    AmbariClient changeAmbariPassword(String userName, String oldPassword, String newPassword, Stack stack, AmbariClient ambariClient)
            throws CloudbreakException {
        try {
            LOGGER.info("Change passwortd for user: {}", userName);
            ambariClient.changePassword(userName, oldPassword, newPassword, true);
        } catch (Exception e) {
            LOGGER.error("Ambari password change failed", e);
        }
        return checkUser(userName, newPassword, stack);
    }

    private AmbariClient checkUser(String userName, String newPassword, Stack stack) throws CloudbreakException {
        AmbariClient ambariClient;
        try {
            ambariClient = ambariClientFactory.getAmbariClient(stack, userName, newPassword);
            ambariClient.getUser(userName);
        } catch (AmbariConnectionException e) {
            LOGGER.error("Can not connect to ambari: ", e);
            throw new CloudbreakException("Can not use user: " + userName, e);
        }
        return ambariClient;
    }
}
