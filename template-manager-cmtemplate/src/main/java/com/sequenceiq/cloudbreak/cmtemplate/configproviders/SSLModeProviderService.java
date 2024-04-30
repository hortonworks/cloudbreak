package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import org.springframework.stereotype.Component;

@Component
public class SSLModeProviderService {

    private SSLModeProviderService() { }

    public static String getSslModeBasedOnConnectionString(String connectionString) {
        return connectionString.contains("verify-ca") ? "sslmode=verify-ca" : "sslmode=verify-full";
    }
}
