package com.sequenceiq.cloudbreak.api.helper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.client.RestClientUtil;

public class HttpHelper {

    private static final HttpHelper INSTANCE = new HttpHelper();

    private final Client client = RestClientUtil.get();

    private HttpHelper() {
    }

    public static HttpHelper getInstance() {
        return INSTANCE;
    }

    public Pair<StatusType, Integer> getContentLength(String url) {
        WebTarget target = client.target(url);
        try (Response response = target.request().head()) {
            return new ImmutablePair<>(response.getStatusInfo(), response.getLength());
        }
    }

    public Pair<StatusType, String> getContent(String url) {
        WebTarget target = client.target(url);
        try (Response response = target.request().get()) {
            StatusType responseStatusInfo = response.getStatusInfo();
            return responseStatusInfo.getFamily().equals(Family.SUCCESSFUL)
                ? new ImmutablePair<>(responseStatusInfo, response.readEntity(String.class))
                : new ImmutablePair<>(responseStatusInfo, null);
        }
    }
}
