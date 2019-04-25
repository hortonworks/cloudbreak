package com.sequenceiq.cloudbreak.service.stack;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;

@Service
public class StackApiViewService {

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    public boolean canChangeCredential(StackApiView stackApiView) {
        if (stackApiView.getStatus() != null) {
            if (stackApiView.getStatus() == Status.AVAILABLE) {
                return !flowLogService.isOtherFlowRunning(stackApiView.getId());
            }
        }
        return false;
    }

    public StackApiView save(StackApiView stackApiView) {
        return stackApiViewRepository.save(stackApiView);
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceId(Long workspaceId, String environmentName, boolean dataLakeOnly) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
        Set<StackApiView> stackViewResponses = StringUtils.isEmpty(environmentName)
                ? getAllByWorkspace(workspaceId, showTerminatedClustersAfterConfig)
                : getAllByWorkspaceAndEnvironment(workspaceId, environmentName, showTerminatedClustersAfterConfig);
        stackViewResponses = filterDatalakes(dataLakeOnly, stackViewResponses);
        return stackViewResponses;
    }

    public StackViewV4Response retrieveById(Long stackId) {
        try {
            return transactionService.required(() -> {
                Optional<StackApiView> byId = stackApiViewRepository.findById(stackId);
                return converterUtil.convert(byId.orElse(null), StackViewV4Response.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Environment decorate(Environment environment, Long workspaceId) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
        if (showTerminatedClustersAfterConfig.isActive()) {
            environment.setStacks(getAllByWorkspaceAndEnvironment(workspaceId, environment.getId(), showTerminatedClustersAfterConfig));
        }
        return environment;
    }

    private Set<StackApiView> filterDatalakes(boolean dataLakeOnly, Set<StackApiView> stackViewResponses) {
        if (dataLakeOnly) {
            stackViewResponses = stackViewResponses.stream()
                    .filter(stackViewResponse ->
                            Boolean.TRUE.equals(stackViewResponse.getCluster().getBlueprint().getTags().getMap().get("shared_services_ready")))
                    .collect(Collectors.toSet());
        }
        return new HashSet<>(stackViewResponses);
    }

    private Set<StackApiView> getAllByWorkspaceAndEnvironment(Long workspaceId, String environmentName,
            ShowTerminatedClustersAfterConfig showTerminatedClustersAfter) {
        Long environmentId = environmentViewService.getIdByName(environmentName, workspaceId);
        return getAllByWorkspaceAndEnvironment(workspaceId, environmentId, showTerminatedClustersAfter);
    }

    private Set<StackApiView> getAllByWorkspaceAndEnvironment(Long workspaceId, Long environmentId,
            ShowTerminatedClustersAfterConfig showTerminatedClustersAfter) {
        return stackApiViewRepository.findAllByWorkspaceIdAndEnvironments(
                workspaceId,
                environmentId,
                showTerminatedClustersAfter.isActive(),
                showTerminatedClustersAfter.showAfterMillisecs()
        );
    }

    private Set<StackApiView> getAllByWorkspace(Long workspaceId, ShowTerminatedClustersAfterConfig showTerminatedClustersAfter) {
        return stackApiViewRepository.findAllByWorkspaceId(
                workspaceId,
                showTerminatedClustersAfter.isActive(),
                showTerminatedClustersAfter.showAfterMillisecs());
    }

    private Set<StackViewV4Response> convertStackViews(Set<StackApiView> stacks) {
        return converterUtil.convertAllAsSet(stacks, StackViewV4Response.class);
    }
}
