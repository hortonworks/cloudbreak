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
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class LiftieConnector implements LiftieApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnector.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    private final String liftiePort;

    private final String liftieAddress;

    private final String liftieProtocol;

    private final Client client;

    private RetriableWebTarget retriableWebTarget;

    public LiftieConnector(@Value("${experience.scan.liftie.api.port}") String liftiePort,
                           @Value("${experience.scan.liftie.api.address}") String liftieAddress,
                           @Value("${experience.scan.protocol}") String liftieProtocol,
                           Client client,
                           RetriableWebTarget retriableWebTarget) {
        this.liftiePort = liftiePort;
        this.liftieAddress = liftieAddress;
        this.liftieProtocol = liftieProtocol;
        this.client = client;
        this.retriableWebTarget = retriableWebTarget;
    }

    @Override
    public ListClustersResponse listClusters(@NotNull String envCrn, @NotNull String tenant, String workloads, Integer page) {
        WebTarget webTarget = client.target(createPathToClusterListingEndpoint());
        Map<String, String> queryParams = new LinkedHashMap<>();
        putIfPresent(queryParams, "status", "ACTIVE"); // TODO: determine statuses we want to get in the list
        putIfPresent(queryParams, "env", envCrn);
        putIfPresent(queryParams,  "tenant", tenant);
        putIfPresent(queryParams, "workloads", workloads);
        putIfPresent(queryParams, "page", page != null ? page.toString() : null);
        webTarget = setQueryParams(webTarget, queryParams);
        Response result = null;
        LOGGER.debug("About to connect Liftie API");
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
        return readResponse(webTarget, result).orElseThrow(() -> new IllegalStateException("Unable to resolve Liftie response!"));
    }

    private WebTarget setQueryParams(WebTarget webTarget, Map<String, String> nameValuePairs) {
        WebTarget target = webTarget;
        for (String key : nameValuePairs.keySet()) {
            String value = nameValuePairs.get(key);
            if (value != null) {
                target = target.queryParam(key, value);
            }
        }
        return target;
    }

    private String createPathToClusterListingEndpoint() {
        return liftieProtocol + "://" + liftieAddress + ":" + liftiePort + "/liftie/api/v1/cluster";
    }

    private Optional<ListClustersResponse> readResponse(WebTarget target, Response response) {
        throwIfNull(response, () -> new IllegalArgumentException("Response should not be null!"));
        ListClustersResponse result = null;
        LOGGER.debug("Going to read response from the Liftie call");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                result = response.readEntity(ListClustersResponse.class);
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
