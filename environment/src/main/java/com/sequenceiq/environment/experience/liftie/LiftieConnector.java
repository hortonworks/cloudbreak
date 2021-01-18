package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.experience.RetriableWebTarget;
import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class LiftieConnector implements LiftieApi {

    private static final String LIFTIE_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve Liftie response!";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnector.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    private final RetriableWebTarget retriableWebTarget;

    private final String liftieBasePath;

    private final Client client;

    public LiftieConnector(@Value("${experience.scan.liftie.api.port}") String liftiePort,
            @Value("${experience.scan.liftie.api.address}") String liftieAddress,
            @Value("${experience.scan.protocol}") String liftieProtocol,
            Client client,
            RetriableWebTarget retriableWebTarget) {

        this.liftieBasePath = String.format("%s://%s:%s/liftie/api/v1", liftieProtocol, liftieAddress, liftiePort);
        this.client = client;
        this.retriableWebTarget = retriableWebTarget;
    }

    @Override
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
        return readResponse(webTarget, result, ListClustersResponse.class).orElseThrow(() -> new IllegalStateException("Unable to resolve Liftie response!"));
    }

    @Override
    public DeleteClusterResponse deleteCluster(@NotNull String clusterId) {
        WebTarget webTarget = client.target(getPathToClusterEndpoint(clusterId));
        LOGGER.debug("About to connect Liftie API to delete cluster {}", clusterId);
        Response result = null;
        try {
            String uuid = UUID.randomUUID().toString();
            Invocation.Builder call = webTarget
                    .request()
                    .accept(APPLICATION_JSON)
                    .header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn())
                    .header(REQUEST_ID_HEADER, uuid);
            result = retriableWebTarget.delete(call);
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the Liftie connection has attempted!", re);
        }
        if (result != null) {
            return readResponse(webTarget, result, DeleteClusterResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } else {
            throw new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG);
        }
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

    private <T> Optional<T> readResponse(WebTarget target, Response response, Class<T> resultClass) {
        throwIfNull(response, () -> new IllegalArgumentException("Response should not be null!"));
        T result = null;
        LOGGER.debug("Going to read response from the Liftie call");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                result = response.readEntity(resultClass);
                LOGGER.debug("Liftie response has resolved.");
            } catch (IllegalStateException | ProcessingException e) {
                String msg = "Failed to resolve response from Liftie on path: " + target.getUri().toString();
                LOGGER.warn(msg, e);
            }
        } else {
            String uri = target.getUri().toString();
            String status = response.getStatusInfo().getReasonPhrase();
            LOGGER.info("Calling Liftie ( on the following path : {} ) was not successful! Status was: {}", uri, status);
        }
        return Optional.ofNullable(result);
    }

}
