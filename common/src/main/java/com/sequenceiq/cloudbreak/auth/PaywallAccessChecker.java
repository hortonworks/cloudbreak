package com.sequenceiq.cloudbreak.auth;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientFactory;

@Component
public class PaywallAccessChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaywallAccessChecker.class);

    @Inject
    private RestClientFactory restClientFactory;

    public void checkPaywallAccess(JsonCMLicense license, String paywallUrl) {
        Client client = restClientFactory.getOrCreateDefault();
        WebTarget target = client.target(paywallUrl);
        HttpAuthenticationFeature basicAuth = HttpAuthenticationFeature.basicBuilder()
                .credentials(license.getPaywallUsername(), license.getPaywallPassword()).build();
        target.register(basicAuth);
        LOGGER.info("Send paywall probe request to {}", paywallUrl);
        String errorMessage = "The Cloudera Manager license is not valid to authenticate to paywall, "
                + "please contact a Cloudera administrator to update it.";
        try (Response response = target.request().get()) {
            int responseStatus = response.getStatus();
            LOGGER.info("Paywall probe response status code: {}", responseStatus);
            if (HttpStatus.OK.value() != responseStatus) {
                throw new BadRequestException(errorMessage);
            }
        } catch (ProcessingException e) {
            LOGGER.info("Cannot send probe request to paywall", e);
            throw new BadRequestException(errorMessage);
        }
    }
}

