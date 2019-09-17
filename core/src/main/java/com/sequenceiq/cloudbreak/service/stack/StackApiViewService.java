package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.converter.stack.StackApiViewToStackViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;

@Service
public class StackApiViewService {

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private StackApiViewToStackViewResponseConverter stackApiViewToStackViewResponseConverter;

    public Set<StackViewResponse> retrieveStackViewsByWorkspaceId(Long workspaceId) {
        try {
            return transactionService.required(() -> {
                Map<Long, Integer> instanceCountMap = Optional.ofNullable(instanceMetaDataRepository.countByWorkspaceId(workspaceId))
                        .orElse(Set.of())
                        .stream()
                        .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));
                return convertStackViews(stackApiViewRepository.findByWorkspaceId(workspaceId), instanceCountMap);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public StackViewResponse retrieveById(Long stackId) {
        try {
            return transactionService.required(() -> {
                Optional<StackApiView> byId = stackApiViewRepository.findById(stackId);
                return conversionService.convert(byId.orElse(null), StackViewResponse.class);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Set<StackViewResponse> convertStackViews(Set<StackApiView> stacks, Map<Long, Integer> instanceCountMap) {
        return Optional.ofNullable(stacks).orElse(Set.of())
                .stream()
                .map(stack -> stackApiViewToStackViewResponseConverter.convert(stack, instanceCountMap.get(stack.getId())))
                .collect(Collectors.toSet());
    }
}
