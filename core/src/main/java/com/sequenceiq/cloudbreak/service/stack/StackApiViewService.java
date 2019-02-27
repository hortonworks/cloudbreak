package com.sequenceiq.cloudbreak.service.stack;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
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

    public Set<StackViewV4Response> retrieveStackViewsByWorkspaceId(Long workspaceId, String environmentName, boolean dataLakeOnly) {
        try {
            Set<StackViewV4Response> stackViewResponses = StringUtils.isEmpty(environmentName)
                    ? getAllByWorkspace(workspaceId)
                    : getAllByWorkspaceAndEnvironment(workspaceId, environmentName);
            stackViewResponses = filterDatalakes(dataLakeOnly, stackViewResponses);
            return stackViewResponses;
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Set<StackViewV4Response> filterDatalakes(boolean dataLakeOnly, Set<StackViewV4Response> stackViewResponses) {
        if (dataLakeOnly) {
            stackViewResponses = stackViewResponses
                    .stream()
                    .filter(stackViewResponse ->
                            Boolean.TRUE.equals(stackViewResponse.getCluster().getAmbari().getClusterDefinition().getTags().get("shared_services_ready")))
                    .collect(Collectors.toSet());
        }
        return new HashSet<>(stackViewResponses);
    }

    private Set<StackViewV4Response> getAllByWorkspaceAndEnvironment(Long workspaceId, String environmentName) throws TransactionExecutionException {
        EnvironmentView env = environmentViewService.getByNameForWorkspaceId(environmentName, workspaceId);
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
        return transactionService.required(() -> convertStackViews(stackApiViewRepository.findAllByWorkspaceIdAndEnvironments(
                workspaceId,
                env,
                showTerminatedClustersAfterConfig.isActive(),
                showTerminatedClustersAfterConfig.showAfterMillisecs()
        )));
    }

    private Set<StackViewV4Response> getAllByWorkspace(Long workspaceId) throws TransactionExecutionException {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();
        return transactionService.required(() -> convertStackViews(stackApiViewRepository.findAllByWorkspaceId(
                workspaceId,
                showTerminatedClustersAfterConfig.isActive(),
                showTerminatedClustersAfterConfig.showAfterMillisecs())));
    }

    private Set<StackViewV4Response> convertStackViews(Set<StackApiView> stacks) {
        return converterUtil.convertAllAsSet(stacks, StackViewV4Response.class);
    }
}
