package com.sequenceiq.cloudbreak.api.helper;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientFactory;

@Component
public class HttpHelper {

    @Inject
    private RestClientFactory restClientFactory;

    public Pair<StatusType, Integer> getContentLength(String url) {
        WebTarget target = restClientFactory.getOrCreateWithFollowRedirects().target(url);
        try (Response response = target.request().head()) {
            return new ImmutablePair<>(response.getStatusInfo(), response.getLength());
        }
    }

    public Pair<StatusType, String> getContent(String url) {
        WebTarget target = restClientFactory.getOrCreateWithFollowRedirects().target(url);
        try (Response response = target.request().get()) {
            StatusType responseStatusInfo = response.getStatusInfo();
            return responseStatusInfo.getFamily().equals(Family.SUCCESSFUL)
                ? new ImmutablePair<>(responseStatusInfo, response.readEntity(String.class))
                : new ImmutablePair<>(responseStatusInfo, null);
        }
    }
}
