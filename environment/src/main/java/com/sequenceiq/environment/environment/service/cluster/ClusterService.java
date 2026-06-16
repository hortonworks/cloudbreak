package com.sequenceiq.environment.environment.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateTrustedRealmRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    private final StackV4Endpoint stackV4Endpoint;

    public ClusterService(StackV4Endpoint stackV4Endpoint) {
        this.stackV4Endpoint = stackV4Endpoint;
    }

    public List<String> getClustersNamesByEncryptionProfile(String encryptionProfileCrn) {
        return stackV4Endpoint.getClustersNamesByEncryptionProfile(0L, encryptionProfileCrn);
    }

    public List<String> getStackCrnsForConfigUpdate(String envCrn) {
        StackViewV4Responses stackViewV4Responses = stackV4Endpoint.list(0L, envCrn, false);
        List<String> responseToLog = Optional.ofNullable(stackViewV4Responses.getResponses()).orElse(List.of()).stream()
                .map(response -> String.format("[Name: %s; Crn: %s; Status: %s, ClusterStatus: %s]",
                        response.getName(), response.getCrn(), response.getStatus(), response.getCluster().getStatus()))
                .collect(Collectors.toList());
        LOGGER.info("Stacks returned for configuration update: {}", responseToLog);

        Predicate<StackViewV4Response> operationalStacks = stack -> AVAILABLE.equals(stack.getStatus());
        Predicate<StackViewV4Response> workloadsOnly = stack -> StackType.WORKLOAD.name().equals(stack.getStackType());

        return stackViewV4Responses.getResponses().stream()
                .filter(operationalStacks)
                .filter(workloadsOnly)
                .map(StackViewV4Response::getCrn)
                .collect(Collectors.toList());
    }

    public List<FlowIdentifier> triggerUpdateTrustedRealmOnDatahubs(String envCrn, UpdateTrustedRealmRequest request) {
        List<String> stackCrns = getStackCrnsForConfigUpdate(envCrn);
        LOGGER.info("Triggering update trusted realm on {} datahub clusters with request: {}", stackCrns.size(), request);
        return stackCrns.stream().map(crn ->
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> stackV4Endpoint.triggerUpdateTrustedRealm(0L, crn, request))
        ).collect(Collectors.toList());
    }

    public List<FlowIdentifier> removeTrustedRealmConfigFromClusters(Optional<EnvironmentDto> environmentDto, String realm) {
        if (environmentDto.isEmpty()) {
            LOGGER.warn("Environment DTO is not present, skipping trusted realm removal from clusters.");
            return List.of();
        }
        EnvironmentDto env = environmentDto.get();
        String envCrn = env.getResourceCrn();

        List<String> stackCrns = getStackCrnsForConfigUpdate(envCrn);
        return stackCrns.stream().map(crn -> {
            LOGGER.info("Triggering async removal of trusted realm '{}' from cluster: {}", realm, crn);
            UpdateTrustedRealmRequest request = new UpdateTrustedRealmRequest();
            request.setRealm(realm);
            request.setRemove(true);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.triggerUpdateTrustedRealm(0L, crn, request));
        }).collect(Collectors.toList());
    }
}
