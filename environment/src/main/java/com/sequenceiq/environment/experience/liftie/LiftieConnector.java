package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.experience.ResponseReader;
import com.sequenceiq.environment.experience.RetriableWebTarget;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.liftie.responses.PageStats;

@Component
public class LiftieConnector implements LiftieApi {

    private static final String LIFTIE_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve Liftie response!";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnector.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    private final RetriableWebTarget retriableWebTarget;

    private final ResponseReader responseReader;

    private final String liftieBasePath;

    private final Client client;

    public LiftieConnector(@Value("${experience.scan.liftie.api.port}") String liftiePort,
            @Value("${experience.scan.liftie.api.address}") String liftieAddress,
            @Value("${experience.scan.protocol}") String liftieProtocol,
            LiftieResponseReader liftieResponseReader,
            RetriableWebTarget retriableWebTarget,
            Client client) {

        this.liftieBasePath = String.format("%s://%s:%s/liftie/api/v1", liftieProtocol, liftieAddress, liftiePort);
        this.retriableWebTarget = retriableWebTarget;
        this.responseReader = liftieResponseReader;
        this.client = client;
    }

    @Override
    @NotNull
    public ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, Integer page) {
        WebTarget webTarget = client.target(getPathToClustersEndpoint());
        Map<String, String> queryParams = new LinkedHashMap<>();
        putIfPresent(queryParams, "env", env);
        putIfPresent(queryParams, "tenant", tenant);
        putIfPresent(queryParams, "page", page != null ? page.toString() : null);
        webTarget = setQueryParams(webTarget, queryParams);
        Response result = null;
        LOGGER.debug("About to connect Liftie API to list clusters");
        try {
            Invocation.Builder call = webTarget
                    .request()
                    .accept(APPLICATION_JSON)
                    .header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn())
                    .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
            result = retriableWebTarget.get(call);
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the Liftie connection has attempted!", re);
        }
        if (result != null) {
            return responseReader.read(webTarget.getUri().toString(), result, ListClustersResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        }
        LOGGER.info("Liftie response was null! I don not know what to do so we are expecting this as a normal behavior ¯\\_(ツ)_/¯ ");
        //TODO: figure out the proper way to handle such a case when the result is null (why can it be null, is it a "good" way of having it, etc.)
        ListClustersResponse empty = new ListClustersResponse();
        PageStats ps = new PageStats();
        ps.setTotalPages(0);
        empty.setPage(ps);
        return empty;
    }

    @Override
    public DeleteClusterResponse deleteCluster(@NotNull String clusterId) {
        WebTarget webTarget = client.target(getPathToClusterEndpoint(clusterId));
        LOGGER.debug("About to connect Liftie API to delete cluster {}", clusterId);
        Response result = null;
        try {
            Invocation.Builder call = webTarget
                    .request()
                    .accept(APPLICATION_JSON)
                    .header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn())
                    .header(REQUEST_ID_HEADER, UUID.randomUUID().toString());
            result = retriableWebTarget.delete(call);
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the Liftie connection has attempted!", re);
        }
        if (result != null) {
            return responseReader.read(webTarget.getUri().toString(), result, DeleteClusterResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        }
        throw new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG);
    }

    private WebTarget setQueryParams(WebTarget webTarget, Map<String, String> nameValuePairs) {
        WebTarget target = webTarget;
        for (Map.Entry<String, String> entry : nameValuePairs.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                target = target.queryParam(entry.getKey(), value);
            }
        }
        return target;
    }

    private String getPathToClustersEndpoint() {
        return liftieBasePath + "/cluster";
    }

    private String getPathToClusterEndpoint(String clusterId) {
        return String.format("%s/%s", getPathToClustersEndpoint(), clusterId);
    }

}
