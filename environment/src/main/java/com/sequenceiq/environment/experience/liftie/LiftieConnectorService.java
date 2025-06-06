package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.QueryParamInjectorUtil;
import com.sequenceiq.environment.experience.ResponseReader;
import com.sequenceiq.environment.experience.RetryableWebTarget;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

@Service
public class LiftieConnectorService implements LiftieApi {

    private static final String LIFTIE_CALL_EXEC_FAILED_MSG = "Something happened while the Kubernetes Experience connection has attempted!";

    private static final String LIFTIE_RESPONSE_RESOLVE_ERROR_MSG = "Unable to find the Kubernetes dependencies of this environment due to internal error.";

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

    @Override
    public ListClustersResponse listClusters(String env, String tenant, String workload, Integer page) {
        LOGGER.debug("About to connect Kubernetes Experience API to list clusters for environment '{}' account '{}' workload '{}', page '{}'",
                env, tenant, workload, page == null ? Integer.valueOf(0) : page);
        WebTarget webTarget = client.target(liftiePathProvider.getPathToClustersEndpoint());
        Map<String, String> queryParams = new LinkedHashMap<>();
        putIfPresent(queryParams, "env", env);
        putIfPresent(queryParams, "tenant", tenant);
        putIfPresent(queryParams, "workloads", workload);
        putIfPresent(queryParams, "page", page != null ? page.toString() : null);
        webTarget = QueryParamInjectorUtil.setQueryParams(webTarget, queryParams);
        LOGGER.info("WebTarget has created for getting Kubernetes clusters related to the given environment environment [name: {}]: {}",
                env, webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        try (Response result = executeCall(webTarget.getUri(), () -> retryableWebTarget.get(call))) {
            return responseReader.read(webTarget.getUri().toString(), result, ListClustersResponse.class)
                    .orElseThrow(() -> new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn(LIFTIE_CALL_EXEC_FAILED_MSG, e);
            throw new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
        }
    }

    @Override
    public DeleteClusterResponse deleteCluster(String clusterId) {
        LOGGER.debug("About to connect Kubernetes Experience API to delete cluster {}", clusterId);
        WebTarget webTarget = client.target(liftiePathProvider.getPathToClusterEndpoint(clusterId));
        LOGGER.info("WebTarget has created for deleting Kubernetes cluster with the following cluster id [id: {}]: {}",
                clusterId, webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        try (Response result = executeCall(webTarget.getUri(), () -> retryableWebTarget.delete(call))) {
            return responseReader.read(webTarget.getUri().toString(), result, DeleteClusterResponse.class)
                    .orElseThrow(() -> new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn(LIFTIE_CALL_EXEC_FAILED_MSG, e);
            throw new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
        }
    }

    @Override
    public ExperiencePolicyResponse getPolicy(String cloudPlatform) {
        if (liftiePathProvider.isPolicyFetchDisabled()) {
            LOGGER.info("Policy fetch for Liftie is not configured.");
            return null;
        }
        WebTarget webTarget = client.target(liftiePathProvider.getPathToPolicyEndpoint(cloudPlatform));
        LOGGER.info("WebTarget has created for getting Kubernetes clusters related minimal policies for cloud platform [platform: {}]: {}",
                cloudPlatform, webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilderForInternalActor(webTarget);
        try (Response result = executeCall(webTarget.getUri(), () -> retryableWebTarget.get(call))) {
            return responseReader.read(webTarget.getUri().toString(), result, ExperiencePolicyResponse.class)
                    .orElseThrow(() -> new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
        } catch (RuntimeException e) {
            LOGGER.warn(LIFTIE_CALL_EXEC_FAILED_MSG, e);
            throw new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG, e);
        }
    }

    private Response executeCall(URI path, Callable<Response> toCall) {
        LOGGER.debug("About to connect to Kubernetes Experience on path: {}", path);
        try {
            return toCall.call();
        } catch (Exception re) {
            LOGGER.warn("Kubernetes Experience http call execution has failed due to:", re);
            throw new ExperienceOperationFailedException(re);
        }
    }

}
