package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;
import static com.sequenceiq.environment.experience.call.retry.ExperienceCallUtil.fetchResultWithRetry;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.QueryParamInjectorUtil;
import com.sequenceiq.environment.experience.ResponseReader;
import com.sequenceiq.environment.experience.call.retry.ExperienceWebTarget;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LiftieConnectorService implements LiftieApi {

    private static final String LIFTIE_RESPONSE_RESOLVE_ERROR_MSG = "Unable to find the Kubernetes dependencies of this environment due to internal error.";

    private static final String LIFTIE_CALL_EXEC_FAILED_MSG = "Something happened while the Kubernetes Experience connection has attempted!";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieConnectorService.class);

    private final InvocationBuilderProvider invocationBuilderProvider;

    private final ExperienceWebTarget experienceWebTarget;

    private final LiftiePathProvider liftiePathProvider;

    private final ResponseReader responseReader;

    private final Client client;

    public LiftieConnectorService(LiftiePathProvider liftiePathProvider, LiftieResponseReader liftieResponseReader, ExperienceWebTarget experienceWebTarget,
            Client client, InvocationBuilderProvider invocationBuilderProvider) {
        this.invocationBuilderProvider = invocationBuilderProvider;
        this.liftiePathProvider = liftiePathProvider;
        this.experienceWebTarget = experienceWebTarget;
        this.responseReader = liftieResponseReader;
        this.client = client;
    }

    @NotNull
    @Override
    public ListClustersResponse listClusters(@NotNull String env, @NotNull String tenant, @Nullable String workload, @Nullable Integer page) {
        LOGGER.debug("About to connect Kubernetes Experience API to list clusters for environment '{}' account '{}' workload '{}', page '{}'",
                env, tenant, workload, page == null ? Integer.valueOf(0) : page);
        javax.ws.rs.client.WebTarget webTarget = client.target(liftiePathProvider.getPathToClustersEndpoint());
        Map<String, String> queryParams = new LinkedHashMap<>();
        putIfPresent(queryParams, "env", env);
        putIfPresent(queryParams, "tenant", tenant);
        putIfPresent(queryParams, "workloads", workload);
        putIfPresent(queryParams, "page", page != null ? page.toString() : null);
        webTarget = QueryParamInjectorUtil.setQueryParams(webTarget, queryParams);
        LOGGER.info("WebTarget has created for getting Kubernetes clusters related to the given environment environment [name: {}]: {}",
                env, webTarget.toString());
        Invocation.Builder callToExecute = invocationBuilderProvider.createInvocationBuilder(webTarget);
        return fetchResultWithRetry(responseReader, webTarget.getUri(), () -> experienceWebTarget.get(callToExecute), ListClustersResponse.class,
                LIFTIE_RESPONSE_RESOLVE_ERROR_MSG)
                .orElseThrow(() -> new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
    }

    @Override
    public DeleteClusterResponse deleteCluster(@NotNull String clusterId) {
        LOGGER.debug("About to connect Kubernetes Experience API to delete cluster {}", clusterId);
        javax.ws.rs.client.WebTarget webTarget = client.target(liftiePathProvider.getPathToClusterEndpoint(clusterId));
        LOGGER.info("WebTarget has created for deleting Kubernetes cluster with the following cluster id [id: {}]: {}",
                clusterId, webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        return fetchResultWithRetry(responseReader, webTarget.getUri(), () -> experienceWebTarget.delete(call), DeleteClusterResponse.class,
                LIFTIE_CALL_EXEC_FAILED_MSG)
                .orElseThrow(() -> new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
    }

    @Override
    public ExperiencePolicyResponse getPolicy(String cloudPlatform) {
        javax.ws.rs.client.WebTarget webTarget = client.target(liftiePathProvider.getPathToPolicyEndpoint(cloudPlatform));
        LOGGER.info("WebTarget has created for getting Kubernetes clusters related minimal policies for cloud platform [platform: {}]: {}",
                cloudPlatform, webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilderForInternalActor(webTarget);
        return fetchResultWithRetry(responseReader, webTarget.getUri(), () -> experienceWebTarget.get(call), ExperiencePolicyResponse.class,
                LIFTIE_CALL_EXEC_FAILED_MSG)
                .orElseThrow(() -> new ExperienceOperationFailedException(LIFTIE_RESPONSE_RESOLVE_ERROR_MSG));
    }

}
