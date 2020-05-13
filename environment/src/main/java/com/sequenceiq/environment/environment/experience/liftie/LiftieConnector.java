package com.sequenceiq.environment.environment.experience.liftie;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.experience.liftie.responses.ListClustersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class LiftieConnector implements LiftieApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnector.class);

    private static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    private final String liftiePort;

    private final String liftieAddress;

    private final String liftieProtocol;

    private final Client client;

    public LiftieConnector(@Value("${liftie.api.port}") String liftiePort,
                           @Value("${liftie.api.address}") String liftieAddress,
                           @Value("${liftie.api.protocol}") String liftieProtocol,
                           Client client) {
        this.liftiePort = liftiePort;
        this.liftieAddress = liftieAddress;
        this.liftieProtocol = liftieProtocol;
        this.client = client;
    }

    @Override
    public ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, String workloads, Integer page) {
        WebTarget webTarget = client.target(createPathToClusterListingEndpoint());
        setQueryParams(webTarget, Map.of("env", env, "tenant", tenant, "workloads", workloads, "page", page));
        Response result = null;
        LOGGER.info("About to connect Liftie API");
        try {
            String uuid = UUID.randomUUID().toString();
            result = webTarget
                    .request()
                    .accept(APPLICATION_JSON)
                    .header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn())
                    .header(REQUEST_ID_HEADER, uuid)
                    .get();
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the Liftie connection has attempted!", re);
        }
        return readResponse(webTarget, result).orElseThrow(() -> new IllegalStateException("Unable to resolve Liftie response!"));
    }

    private void setQueryParams(WebTarget webTarget, Map<String, Object> nameValuePairs) {
        nameValuePairs.forEach((name, value) -> {
            if (value != null) {
                webTarget.queryParam(name, value);
            }
        });
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
