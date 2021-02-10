package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.ResponseReader;
import com.sequenceiq.environment.experience.RetryableWebTarget;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Service
public class LiftieConnectorService implements LiftieApi {

    private static final String LIFTIE_CALL_EXEC_FAILED_MSG = "Something happened while the Liftie connection has attempted!";

    private static final String LIFTIE_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve Liftie response!";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnectorService.class);

    private final InvocationBuilderProvider invocationBuilderProvider;

    private final RetryableWebTarget retryableWebTarget;

    private final LiftiePathProvider liftiePathProvider;

    private final ResponseReader responseReader;

    private final Client client;

    public LiftieConnectorService(LiftiePathProvider liftiePathProvider, LiftieResponseReader liftieResponseReader, RetryableWebTarget retryableWebTarget,
            Client client, InvocationBuilderProvider invocationBuilderProvider) {
        this.invocationBuilderProvider = invocationBuilderProvider;
        this.liftiePathProvider = liftiePathProvider;
        this.retryableWebTarget = retryableWebTarget;
        this.responseReader = liftieResponseReader;
        this.client = client;
    }

    @NotNull
    @Override
    public ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, @Nullable String workload, @Nullable Integer page) {
        LOGGER.debug("About to connect Liftie API to list clusters");
        WebTarget webTarget = client.target(liftiePathProvider.getPathToClustersEndpoint());
        Map<String, String> queryParams = new LinkedHashMap<>();
        putIfPresent(queryParams, "env", env);
        putIfPresent(queryParams, "tenant", tenant);
        putIfPresent(queryParams, "workloads", workload);
        putIfPresent(queryParams, "page", page != null ? page.toString() : null);
        webTarget = setQueryParams(webTarget, queryParams);
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        try (Response result = executeCall(webTarget.getUri(), () -> retryableWebTarget.get(call))) {
            return responseReader.read(webTarget.getUri().toString(), result, ListClustersResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn(LIFTIE_CALL_EXEC_FAILED_MSG, e);
            throw new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
        }
    }

    @Override
    public DeleteClusterResponse deleteCluster(@NotNull String clusterId) {
        LOGGER.debug("About to connect Liftie API to delete cluster {}", clusterId);
        WebTarget webTarget = client.target(liftiePathProvider.getPathToClusterEndpoint(clusterId));
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        try (Response result = executeCall(webTarget.getUri(), () -> retryableWebTarget.delete(call))) {
            return responseReader.read(webTarget.getUri().toString(), result, DeleteClusterResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn(LIFTIE_CALL_EXEC_FAILED_MSG, e);
            throw new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
        }
    }

    private Response executeCall(URI path, Callable<Response> toCall) {
        LOGGER.debug("About to connect to Liftie on path: {}", path);
        try {
            return toCall.call();
        } catch (Exception re) {
            LOGGER.warn("Liftie http call execution has failed due to:", re);
            throw new IllegalStateException(re);
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

}
