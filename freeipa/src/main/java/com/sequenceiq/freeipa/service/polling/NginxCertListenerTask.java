package com.sequenceiq.freeipa.service.polling;

import java.security.cert.X509Certificate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Component
public class NginxCertListenerTask implements StatusCheckerTask<NginxPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NginxCertListenerTask.class);

    @Override
    public boolean checkStatus(NginxPollerObject nginxPollerObject) {
        boolean nginxAvailable = false;
        String ip = nginxPollerObject.getIp();
        int port = nginxPollerObject.getGatewayPort();
        LOGGER.debug("Check if nginx is running on {}:{}.", ip, port);
        try {
            Client client = nginxPollerObject.getClient();
            WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, port));
            nginxTarget.path("/").request().get().close();
            X509Certificate[] chain = nginxPollerObject.getTrustManager().getChain();
            if (chain == null || chain.length == 0) {
                LOGGER.debug("Nginx is listening on {}:{}, but TLS is not configured yet", ip, port);
            } else {
                nginxAvailable = true;
            }
        } catch (Exception e) {
            LOGGER.debug("Nginx is not listening on {}:{}, error: {}", ip, port, e.getMessage());
        }
        return nginxAvailable;
    }

    @Override
    public void handleTimeout(NginxPollerObject nginxPollerObject) {
        throw new CloudbreakServiceException("Operation timed out. Failed to check nginx.");
    }

    @Override
    public String successMessage(NginxPollerObject nginxPollerObject) {
        return "Nginx startup finished with success result.";
    }

    @Override
    public boolean exitPolling(NginxPollerObject nginxPollerObject) {
        return false;
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.error("Nginx error", e);
    }
}
