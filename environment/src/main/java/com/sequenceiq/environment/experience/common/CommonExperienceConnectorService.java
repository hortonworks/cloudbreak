package com.sequenceiq.environment.experience.common;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.QueryParamInjectorUtil;
import com.sequenceiq.environment.experience.ResponseReader;
import com.sequenceiq.environment.experience.RetryableWebTarget;
import com.sequenceiq.environment.experience.api.CommonExperienceApi;
import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.common.responses.CpInternalEnvironmentResponse;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

@Service
public class CommonExperienceConnectorService implements CommonExperienceApi {

    private static final String COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve the experience's response!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperienceConnectorService.class);

    private final CommonExperienceWebTargetProvider commonExperienceWebTargetProvider;

    private final InvocationBuilderProvider invocationBuilderProvider;

    private final RetryableWebTarget retryableWebTarget;

    private final ResponseReader responseReader;

    public CommonExperienceConnectorService(RetryableWebTarget retryableWebTarget, CommonExperienceResponseReader commonExperienceResponseReader,
            CommonExperienceWebTargetProvider commonExperienceWebTargetProvider, InvocationBuilderProvider invocationBuilderProvider) {
        this.commonExperienceWebTargetProvider = commonExperienceWebTargetProvider;
        this.invocationBuilderProvider = invocationBuilderProvider;
        this.responseReader = commonExperienceResponseReader;
        this.retryableWebTarget = retryableWebTarget;
    }

    @NotNull
    @Override
    public Set<CpInternalCluster> getExperienceClustersConnectedToEnv(String experienceBasePath, String environmentCrn) {
        WebTarget webTarget = commonExperienceWebTargetProvider.createWebTargetForClusterFetch(experienceBasePath, environmentCrn);
        LOGGER.info("WebTarget has created for getting workspaces for environment [crn: {}]: {}",
                environmentCrn, webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        Optional<Response> result = executeCall(webTarget.getUri(), () -> retryableWebTarget.get(call));
        if (result.isPresent()) {
            Status status = Status.fromStatusCode(result.get().getStatus());
            if (status != NOT_FOUND) {
                Optional<CpInternalEnvironmentResponse> response = responseReader
                        .read(webTarget.getUri().toString(), result.get(), CpInternalEnvironmentResponse.class);
                return response.map(CpInternalEnvironmentResponse::getResults)
                        .orElseThrow(() -> new ExperienceOperationFailedException(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG));
            } else {
                LOGGER.info("The response's status was [{}], but for cluster fetch this is not acceptable, therefore an empty set will be returned",
                        status.name());
                return Set.of();
            }
        }
        throw new ExperienceOperationFailedException(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG);
    }

    @Override
    public void deleteWorkspaceForEnvironment(String experienceBasePath, String environmentCrn, boolean force) {
        WebTarget webTarget = commonExperienceWebTargetProvider.createWebTargetForClusterFetch(experienceBasePath, environmentCrn);
        LOGGER.info("WebTarget has created for deleting workspaces for environment [crn: {}]: {}",
                environmentCrn, webTarget.toString());
        if (force) {
            LOGGER.debug("Force deletion was requested thus request path is about to be decorated accordingly.");
            webTarget = QueryParamInjectorUtil.setQueryParams(webTarget, Map.of("force", "true"));
        }
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilder(webTarget);
        executeCall(webTarget.getUri(), () -> retryableWebTarget.delete(call))
                .ifPresentOrElse(this::throwExceptionIfResultIsUnsuccessful, this::throwExperienceOperationFailedException);
    }

    @Override
    public ExperiencePolicyResponse collectPolicy(String experienceBasePath, String cloudPlatform) {
        WebTarget webTarget = commonExperienceWebTargetProvider.createWebTargetForPolicyFetch(experienceBasePath, cloudPlatform);
        LOGGER.info("WebTarget has created for collecting policies: {}", webTarget.toString());
        Invocation.Builder call = invocationBuilderProvider.createInvocationBuilderForInternalActor(webTarget);
        Optional<Response> response = executeCall(webTarget.getUri(), () -> retryableWebTarget.get(call));
        if (response.isPresent()) {
            return responseReader.read(webTarget.getUri().toString(), response.get(), ExperiencePolicyResponse.class)
                    .orElseThrow(() -> new ExperienceOperationFailedException(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG));
        }
        throw new ExperienceOperationFailedException(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG);
    }

    private void throwExceptionIfResultIsUnsuccessful(Response response) {
        if (!response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            LOGGER.info("Experience deletion result was not {} but {} with the reason of: {}",
                    SUCCESSFUL.name(), response.getStatusInfo().getFamily().name(), response.getStatusInfo().getReasonPhrase());
            throw new ExperienceOperationFailedException("We are unable to delete connected experience(s), please retry or check your experiences manually.");
        }
    }

    private void throwExperienceOperationFailedException() {
        throw new ExperienceOperationFailedException(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG);
    }

    private Optional<Response> executeCall(URI path, Callable<Response> toCall) {
        LOGGER.debug("About to connect to experience on path: {}", path.toString());
        try {
            return Optional.ofNullable(toCall.call());
        } catch (Exception re) {
            LOGGER.warn("Something happened while the experience connection attempted!", re);
        }
        return Optional.empty();
    }

}
