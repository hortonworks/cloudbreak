package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class StackApiViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewService.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    public StackApiViewService() {
    }

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
            LOGGER.info("Environment name was empty so we will query all the stack.");
            stackViewResponses = getAllByWorkspace(workspaceId, showTerminatedClustersAfterConfig);
        } else {
            LOGGER.info("Environment name was defined so we will query all the stack in the {} environment.", environmentName);
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(environmentName);
            stackViewResponses = getAllByWorkspaceAndEnvironment(workspaceId, environmentResponse.getCrn(), showTerminatedClustersAfterConfig);
        }

        if (stackType != null) {
            LOGGER.info("Stacktype {} was defined so we are filtering the stacks.", stackType);
            stackViewResponses = filterByStackType(stackType, stackViewResponses);
        }
        return stackViewResponses;
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(Long workspaceId, String environmentCrn, @Nullable StackType stackType) {
        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = showTerminatedClusterConfigService.get();

        Set<StackApiView> stackViewResponses;
        if (StringUtils.isEmpty(environmentCrn)) {
            LOGGER.info("Environment crn was empty so we will query all the stack.");
            stackViewResponses = getAllByWorkspace(workspaceId, showTerminatedClustersAfterConfig);
        } else {
            LOGGER.info("Environment crn was defined so we will query all the stack in the {} environment.", environmentCrn);
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByCrn(environmentCrn);
            stackViewResponses = getAllByWorkspaceAndEnvironment(workspaceId, environmentResponse.getCrn(), showTerminatedClustersAfterConfig);
        }

        if (stackType != null) {
            LOGGER.info("Stacktype {} was defined so we are filtering the stacks.", stackType);
            stackViewResponses = filterByStackType(stackType, stackViewResponses);
        }
        return stackViewResponses;
    }

    @PreAuthorize("hasRole('INTERNAL')")
    public StackApiView retrieveStackByCrnAndType(String crn, StackType stackType) {
        return stackApiViewRepository.findByResourceCrnAndStackType(crn, stackType).orElseThrow(notFound("Stack", crn));
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
}
