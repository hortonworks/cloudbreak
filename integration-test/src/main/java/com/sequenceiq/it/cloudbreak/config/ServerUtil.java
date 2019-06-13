package com.sequenceiq.it.cloudbreak.config;

import static org.apache.commons.net.util.Base64.decodeBase64;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ServerUtil {

    String calculateCrnsFromProfile(String userCrn, Map<String, String> prof) {
        return StringUtils.isEmpty(userCrn)
                ? new String(decodeBase64(prof.get("apikeyid")))
                : userCrn;
    }

    String calculateServerAddressFromProfile(String server, Map<String, String> prof, int port) {
        return StringUtils.isEmpty(server)
                ? enforceHttpForServerAddress(prof.get("server"), port)
                : server;
    }

    private String enforceHttpForServerAddress(String serverRaw, int port) {
        return "http://" + getDomainFromUrl(serverRaw) + ":" + port;
    }

    private String getDomainFromUrl(String url) {
        if ("localhost".equals(url)) {
            return url;
        }
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid configuration value in Profile for server " + url, e);
        }
    }
}
