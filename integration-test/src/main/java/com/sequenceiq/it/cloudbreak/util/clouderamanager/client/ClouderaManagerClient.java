package com.sequenceiq.it.cloudbreak.util.clouderamanager.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;

@Component
public class ClouderaManagerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClient.class);

    @Inject
    private CmRequestIdProviderInterceptor cmRequestIdProviderInterceptor;

    public ClouderaManagerClient() {
    }

    public ApiClient getCmApiClient(String serverFqdn, String apiVersion, String cmUser, String cmPassword) {
        LOGGER.info(String.format("Cloudera Manager Server access details: %nserverFqdn: %s %ncmUser: %s %ncmPassword: %s", serverFqdn, cmUser, cmPassword));
        ApiClient cmClient = new ApiClient();
        cmClient.setBasePath("https://" + serverFqdn + "/clouderamanager" + apiVersion);
        cmClient.setUsername(cmUser);
        cmClient.setPassword(cmPassword);
        cmClient.setVerifyingSsl(false);
        cmClient.getHttpClient().interceptors().add(cmRequestIdProviderInterceptor);
        LOGGER.info(String.format("Cloudera Manager Base Path: %s", cmClient.getBasePath()));
        return cmClient;
    }
}
