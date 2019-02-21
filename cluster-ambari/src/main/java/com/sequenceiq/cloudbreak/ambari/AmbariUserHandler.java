package com.sequenceiq.cloudbreak.ambari;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class AmbariUserHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariUserHandler.class);

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Retryable(value = CloudbreakException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    AmbariClient createAmbariUser(String newUserName, String newPassword, Stack stack, AmbariClient ambariClient, HttpClientConfig clientConfig)
            throws CloudbreakException {
        try {
            LOGGER.debug("Create user with username: {}", newUserName);
            ambariClient.createUser(newUserName, newPassword, true);
        } catch (Exception e) {
            LOGGER.warn("Can not create ambari user", e);
        }
        return checkUser(newUserName, newPassword, stack, clientConfig);
    }

    @Retryable(value = CloudbreakException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    AmbariClient changeAmbariPassword(String userName, String oldPassword, String newPassword, Stack stack, AmbariClient ambariClient,
            HttpClientConfig clientConfig) throws CloudbreakException {
        try {
            LOGGER.debug("Change passwortd for user: {}", userName);
            ambariClient.changePassword(userName, oldPassword, newPassword, true);
        } catch (Exception e) {
            LOGGER.warn("Ambari password change failed", e);
        }
        return checkUser(userName, newPassword, stack, clientConfig);
    }

    private AmbariClient checkUser(String userName, String newPassword, Stack stack, HttpClientConfig clientConfig) throws CloudbreakException {
        AmbariClient ambariClient;
        try {
            ambariClient = ambariClientFactory.getAmbariClient(stack, userName, newPassword, clientConfig);
            ambariClient.getUser(userName);
        } catch (AmbariConnectionException e) {
            LOGGER.info("Can not connect to ambari: ", e);
            throw new CloudbreakException("Can not use user: " + userName, e);
        }
        return ambariClient;
    }
}
