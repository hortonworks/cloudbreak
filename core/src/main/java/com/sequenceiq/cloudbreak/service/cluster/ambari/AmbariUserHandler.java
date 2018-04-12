package com.sequenceiq.cloudbreak.service.cluster.ambari;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class AmbariUserHandler {

    @Inject
    private AmbariClientFactory ambariClientFactory;

    public AmbariClient createAmbariUser(String newUserName, String newPassword, Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        try {
            ambariClient.createUser(newUserName, newPassword, true);
        } catch (Exception e) {
            try {
                ambariClient = ambariClientFactory.getAmbariClient(stack, newUserName, newPassword);
                ambariClient.ambariServerVersion();
            } catch (Exception ignored) {
                throw new CloudbreakException(e);
            }
        }
        return ambariClient;
    }
}
