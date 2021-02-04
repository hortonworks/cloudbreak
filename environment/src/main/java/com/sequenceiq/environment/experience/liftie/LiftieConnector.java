package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.ResponseReader;
import com.sequenceiq.environment.experience.RetryableWebTarget;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class LiftieConnector implements LiftieApi {

    private static final String LIFTIE_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve Liftie response!";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnector.class);

    private final InvocationBuilderProvider invocationBuilderProvider;

    private final RetryableWebTarget retryableWebTarget;

    private final LiftiePathProvider liftiePathProvider;

    private final ResponseReader responseReader;

    private final Client client;

    public LiftieConnector(LiftiePathProvider liftiePathProvider, LiftieResponseReader liftieResponseReader, RetryableWebTarget retryableWebTarget,
            Client client, InvocationBuilderProvider invocationBuilderProvider) {
        this.invocationBuilderProvider = invocationBuilderProvider;
        this.liftiePathProvider = liftiePathProvider;
        this.retryableWebTarget = retryableWebTarget;
        this.responseReader = liftieResponseReader;
        this.client = client;
    }

    @Override
    public @NotNull ListClustersResponse listPagedClustersWithWorkloadFilter(@NotNull String env, @NotNull String tenant, @Nullable Integer page,
            @Nullable String workload) {
        return listClusters(env, tenant, workload, page);
    }

    @Override
    public @NotNull ListClustersResponse listClustersWithWorkloadFilter(@NotNull String env, @NotNull String tenant, @Nullable String workload) {
        return listClusters(env, tenant, workload, null);
    }

    @Override
    public @NotNull ListClustersResponse listPagedClusters(@NotNull String env, @NotNull String tenant, @Nullable Integer page) {
        return listClusters(env, tenant, null, page);
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
        try (Response result = retryableWebTarget.get(call)) {
            return responseReader.read(webTarget.getUri().toString(), result, ListClustersResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn("Something happened while the Liftie connection has attempted!", e);
            throw new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
        }
    }

    @Override
    public DeleteClusterResponse deleteCluster(@NotNull String clusterId) {
        LOGGER.debug("About to connect Liftie API to delete cluster {}", clusterId);
        WebTarget webTarget = client.target(liftiePathProvider.getPathToClusterEndpoint(clusterId));
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        try (Response result = retryableWebTarget.delete(call)) {
            return responseReader.read(webTarget.getUri().toString(), result, DeleteClusterResponse.class)
                    .orElseThrow(() -> new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn("Something happened while the Liftie connection has attempted!", e);
            throw new IllegalStateException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
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
