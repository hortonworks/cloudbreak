package com.sequenceiq.cloudbreak.service.stack.flow;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationFailedException;

@Component
public class NginxCertListenerTask extends StackBasedStatusCheckerTask<NginxPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NginxCertListenerTask.class);

    @Override
    public boolean checkStatus(NginxPollerObject nginxPollerObject) {
        boolean nginxAvailable = false;
        String ip = nginxPollerObject.getIp();
        int port = nginxPollerObject.getGatewayPort();
        LOGGER.info("Check if nginx is running on {}:{}.", ip, port);
        try {
            Client client = nginxPollerObject.getClient();
            WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, port));
            nginxTarget.path("/").request().get();
            nginxAvailable = true;
        } catch (Exception e) {
            LOGGER.info("Nginx is not listening on {}:{}, error: {}", ip, port, e.getMessage());
        }
        return nginxAvailable;
    }

    @Override
    public void handleTimeout(NginxPollerObject nginxPollerObject) {
        throw new AmbariOperationFailedException("Operation timed out. Failed to check nginx.");
    }

    @Override
    public String successMessage(NginxPollerObject nginxPollerObject) {
        return "Nginx startup finished with success result.";
    }
}
