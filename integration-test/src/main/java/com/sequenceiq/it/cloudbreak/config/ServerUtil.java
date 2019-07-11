package com.sequenceiq.it.cloudbreak.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ServerUtil {

    String calculateApiKeyFromProfile(String apikeyid, Map<String, String> prof) {
        return StringUtils.isEmpty(apikeyid)
                ? prof.get("apikeyid")
                : apikeyid;
    }

    String calculatePrivateKeyFromProfile(String privatekey, Map<String, String> prof) {
        return StringUtils.isEmpty(privatekey)
                ? prof.get("privatekey")
                : privatekey;
    }

    String calculateServerAddressFromProfile(String server, Map<String, String> prof) {
        return StringUtils.isEmpty(server)
                ? enforceHttpForServerAddress(prof.get("server"))
                : server;
    }

    String calculatePureServerAddressFromProfile(String server, Map<String, String> prof, int port) {
        return StringUtils.isEmpty(server)
                ? enforceHttpForServerAddress(prof.get("server"), port)
                : server;
    }

    private String enforceHttpForServerAddress(String serverRaw) {
        return "http://" + getDomainFromUrl(serverRaw);
    }

    private String enforceHttpsForServerAddress(String serverRaw) {
        return "https://" + getDomainFromUrl(serverRaw);
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
