package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

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
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public boolean canChangeCredential(StackApiView stackApiView) {
        if (stackApiView.getStatus() != null) {
            if (stackApiView.getStatus() == Status.AVAILABLE) {
                if (flowLogService.isOtherFlowRunning(stackApiView.getId())) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public StackApiView save(StackApiView stackApiView) {
        return stackApiViewRepository.save(stackApiView);
    }

    public Set<StackViewResponse> retrieveStackViewsByWorkspaceId(Long workspaceId, String environmentName, boolean dataLakeOnly) {
        try {
            Set<StackViewResponse> stackViewResponses;
            if (StringUtils.isEmpty(environmentName)) {
                stackViewResponses = transactionService.required(() ->
                        convertStackViews(stackApiViewRepository.findByWorkspaceId(workspaceId)));
            } else {
                EnvironmentView env = environmentViewService.getByNameForWorkspaceId(environmentName, workspaceId);
                stackViewResponses = transactionService.required(() ->
                        convertStackViews(stackApiViewRepository.findAllByWorkspaceIdAndEnvironments(workspaceId, env)));
            }
            if (dataLakeOnly) {
                stackViewResponses = stackViewResponses
                        .stream()
                        .filter(stackViewResponse ->
                                Optional.ofNullable((Boolean) stackViewResponse.getCluster().getBlueprint().getTags().get("shared_services_ready"))
                                        .orElse(false))
                        .collect(Collectors.toSet());
            }
            return stackViewResponses;
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Set<StackViewResponse> convertStackViews(Set<StackApiView> stacks) {
        return (Set<StackViewResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackViewResponse.class)));
    }
}
