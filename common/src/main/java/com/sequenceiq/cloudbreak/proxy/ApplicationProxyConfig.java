package com.sequenceiq.cloudbreak.proxy;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Objects;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProxyConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProxyConfig.class);

    @Value("${https.proxyHost:}")
    private String httpsProxyHost;

    @Value("${https.proxyPort:}")
    private String httpsProxyPort;

    @Value("${https.proxyUser:}")
    private String httpsProxyUser;

    @Value("${https.proxyPassword:}")
    private String httpsProxyPassword;

    @Value("${https.proxyForClusterConnection:false}")
    private boolean useProxyForClusterConnection;

    @PostConstruct
    public void init() {
        if (isProxyAuthRequired()) {
            LOGGER.debug("Configure the JVM default authenticator for proxy: {}:{} with user: {}", httpsProxyHost, httpsProxyPort, httpsProxyUser);
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        if (getRequestingHost().equalsIgnoreCase(httpsProxyHost)) {
                            if (Objects.equals(getRequestingPort(), getHttpsProxyPort())) {
                                return new PasswordAuthentication(httpsProxyUser, httpsProxyPassword.toCharArray());
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    public String getHttpsProxyHost() {
        return httpsProxyHost;
    }

    public int getHttpsProxyPort() {
        return Integer.parseInt(httpsProxyPort);
    }

    public String getHttpsProxyUser() {
        return httpsProxyUser;
    }

    public String getHttpsProxyPassword() {
        return httpsProxyPassword;
    }

    public boolean isUseProxyForClusterConnection() {
        return isHttpsProxyConfigured() && useProxyForClusterConnection;
    }

    public boolean isProxyAuthRequired() {
        return isHttpsProxyConfigured() && isNoneBlank(httpsProxyUser) && isNoneBlank(httpsProxyPassword);
    }

    private boolean isHttpsProxyConfigured() {
        return isNoneBlank(httpsProxyHost) && isNoneBlank(httpsProxyPort);
    }
}
