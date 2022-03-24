package com.sequenceiq.cloudbreak.auth;

import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaywallCredentialPopulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaywallCredentialPopulator.class);

    private static final Pattern URL_PATTERN = Pattern.compile("http[s]?://archive\\.cloudera\\.com.+");

    @Inject
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    public void populateWebTarget(String baseUrl, WebTarget target) {
        if (URL_PATTERN.matcher(baseUrl).find()) {
            LOGGER.debug("Adding Paywall credential to request.");
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            JsonCMLicense license = clouderaManagerLicenseProvider.getLicense(userCrn);
            HttpAuthenticationFeature basicAuth = createAuthFeature(license);
            target.register(basicAuth);
        } else {
            LOGGER.debug("Skip adding paywall credentials to request because the URL is not eligible {}", baseUrl);
        }
    }

    private HttpAuthenticationFeature createAuthFeature(JsonCMLicense license) {
        return HttpAuthenticationFeature.basicBuilder()
                .credentials(license.getPaywallUsername(), license.getPaywallPassword())
                .build();
    }
}
