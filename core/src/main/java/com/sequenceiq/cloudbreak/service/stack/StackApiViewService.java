package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class StackApiViewService {

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private EnvironmentClientService environmentClientService;

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

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentName(Long workspaceId, String environmentName, @Nullable StackType stackType) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();

        Set<StackApiView> stackViewResponses;
        if (StringUtils.isEmpty(environmentName)) {
            stackViewResponses = getAllByWorkspace(workspaceId, showTerminatedClustersAfterConfig);
        } else {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(environmentName);
            stackViewResponses = getAllByWorkspaceAndEnvironment(workspaceId, environmentResponse.getCrn(), showTerminatedClustersAfterConfig);
        }

        if (stackType != null) {
            stackViewResponses = filterByStackType(stackType, stackViewResponses);
        }
        return stackViewResponses;
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(Long workspaceId, String environmentCrn, @Nullable StackType stackType) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();

        Set<StackApiView> stackViewResponses;
        if (StringUtils.isEmpty(environmentCrn)) {
            stackViewResponses = getAllByWorkspace(workspaceId, showTerminatedClustersAfterConfig);
        } else {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByCrn(environmentCrn);
            stackViewResponses = getAllByWorkspaceAndEnvironment(workspaceId, environmentResponse.getCrn(), showTerminatedClustersAfterConfig);
        }

        if (stackType != null) {
            stackViewResponses = filterByStackType(stackType, stackViewResponses);
        }
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

    private Set<StackApiView> filterByStackType(StackType stackType, Set<StackApiView> stackViewResponses) {
        return stackViewResponses.stream()
                .filter(stackViewResponse -> stackType == stackViewResponse.getType())
                .collect(Collectors.toSet());
    }

    private Set<StackApiView> getAllByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn,
            ShowTerminatedClustersAfterConfig showTerminatedClustersAfter) {
        return stackApiViewRepository.findAllByWorkspaceIdAndEnvironments(
                workspaceId,
                environmentCrn,
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
