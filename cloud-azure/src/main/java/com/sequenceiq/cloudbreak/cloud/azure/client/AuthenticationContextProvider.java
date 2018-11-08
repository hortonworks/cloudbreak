package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import com.microsoft.aad.adal4j.AuthenticationContext;

@Component
public class AuthenticationContextProvider {

    public AuthenticationContext getAuthenticationContext(@NotNull String authority, boolean validateAuthority, @NotNull ExecutorService service)
                    throws MalformedURLException {
        return new AuthenticationContext(authority, validateAuthority, service);
    }

}
