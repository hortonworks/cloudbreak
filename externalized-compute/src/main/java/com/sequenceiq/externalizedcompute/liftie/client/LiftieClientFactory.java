package com.sequenceiq.externalizedcompute.liftie.client;

import com.sequenceiq.externalizedcompute.ApiClient;

public class LiftieClientFactory {

    private LiftieClientFactory() {
    }

    public static ApiClient getApiClient(LiftieEndpoint liftieEndpoint) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(liftieEndpoint.getBasePath());
        return apiClient;
    }
}
