package org.apache.cb.yarn.service.api;

import org.apache.cb.yarn.service.api.auth.ApiKeyAuth;
import org.apache.cb.yarn.service.api.impl.DefaultApi;

import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;

public class Main {

    public static void main(String[] args) throws ApiException {
        ApiKeyAuth auth = new ApiKeyAuth(ApiKeyLocation.QUERY.toValue(), "user.name");
        auth.setApiKey("cbtest");
        ApiClient apiClient = new ApiClient().setBasePath("http://y001.l42scl.hortonworks.com:8088");
        apiClient.getAuthentications().put("test", auth);
        DefaultApi api = new DefaultApi(apiClient);
        Object service = api.appV1ServicesServiceNameGet("httpd-docker-1");
        System.out.println(service);
    }
}
