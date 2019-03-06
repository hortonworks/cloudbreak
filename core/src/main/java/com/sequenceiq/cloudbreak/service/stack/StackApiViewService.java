package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;

@Service
public class StackApiViewService {

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public Set<StackViewResponse> retrieveStackViewsByWorkspaceId(Long workspaceId) {
        try {
            return transactionService.required(() ->
                    convertStackViews(stackApiViewRepository.findByWorkspaceId(workspaceId)));
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

    private Set<StackViewResponse> convertStackViews(Set<StackApiView> stacks) {
        return (Set<StackViewResponse>) conversionService.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(StackViewResponse.class)));
    }
}
