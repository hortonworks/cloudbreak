package com.sequenceiq.cloudbreak.service.credential;

import static java.util.Collections.singletonMap;

import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Component
public class PaywallCredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaywallCredentialService.class);

    private static final int SHIFT = 3;

    @Value("${cb.paywall.username:}")
    private String paywallUserName;

    @Value("${cb.paywall.password:}")
    private String paywallPassword;

    public String addCredentialForUrl(String url) {
        if (paywallCredentialAvailable()) {
            StringBuilder stringBuilder = new StringBuilder(url);
            stringBuilder.insert(url.indexOf("://") + SHIFT, String.format("%s:%s@", paywallUserName, paywallPassword));
            url = stringBuilder.toString();
        }
        return url;
    }

    public boolean paywallCredentialAvailable() {
        boolean result = !paywallUserName.isEmpty() && !paywallPassword.isEmpty();
        if (result) {
            LOGGER.info("Paywall username and password is available.");
        } else {
            LOGGER.info("Paywall username and password is not available");
        }
        return result;
    }

    public void getPaywallCredential(Map<String, SaltPillarProperties> servicePillar) {
        servicePillar.put("paywall", new SaltPillarProperties("/hdp/paywall.sls", singletonMap("paywall", createCredential())));
    }

    public String getBasicAuthorizationEncoded() {
        return Base64.getEncoder().encodeToString(String.format("%s:%s", paywallUserName, paywallPassword).getBytes());
    }

    private Map<String, String> createCredential() {
        return Map.of(
                "paywallUser", paywallUserName,
                "paywallPassword", paywallPassword);
    }
}
