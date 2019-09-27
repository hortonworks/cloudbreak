package com.sequenceiq.it.cloudbreak.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ServerUtil {

    String calculateApiKeyFromProfile(String apiKeyId, Map<String, String> prof) {
        String apiKeyIdFromProfile = prof.get("apikeyid");
        return StringUtils.isEmpty(apiKeyIdFromProfile)
                ? apiKeyId
                : apiKeyIdFromProfile;
    }

    String calculatePrivateKeyFromProfile(String privateKey, Map<String, String> prof) {
        String privateKeyFromProfile = prof.get("privatekey");
        return StringUtils.isEmpty(privateKeyFromProfile)
                ? privateKey
                : privateKeyFromProfile;
    }

    String calculateServerAddressFromProfile(String server, Map<String, String> prof) {
        String serverRaw = prof.get("server");
        return StringUtils.isEmpty(serverRaw)
                ? server
                : enforceHttpsForServerAddress(serverRaw);
    }

    String enforceHttpsForServerAddress(String serverRaw) {
        //Hack to avoid SSLException in local mock/e2e tests due to CBD modification that allows traffic to services without https
        if ("localhost".equals(serverRaw) || "http://localhost".equals(serverRaw)) {
            return "http://localhost";
        }
        return "https://" + getDomainFromUrl(serverRaw);
    }

    private String getDomainFromUrl(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid configuration value in Profile for server " + url, e);
        }
    }
}
