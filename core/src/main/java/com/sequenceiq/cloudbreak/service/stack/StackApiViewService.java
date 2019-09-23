package com.sequenceiq.cloudbreak.service.stack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.converter.stack.StackListItemToStackViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;

@Service
public class StackApiViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewService.class);

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private StackListItemToStackViewResponseConverter stackListItemToStackViewResponseConverter;

    public Set<StackViewResponse> retrieveStackViewsByWorkspaceId(Long workspaceId) {
        Map<Long, Integer> stackInstanceCounts = instanceMetaDataRepository.countByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));
        Map<Long, Integer> stackUnhealthyInstanceCounts = hostMetadataRepository.countUnhealthyByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));
        Set<StackListItem> stackList = stackRepository.findByWorkspaceId(workspaceId);
        return stackList.stream()
                .map(item -> {
                    String sharedClusterName = Optional.ofNullable(item.getSharedClusterId())
                            .map(sharedClusterId -> stackRepository.findNameByIdAndWorkspaceId(sharedClusterId, workspaceId)).orElse(null);
                    return stackListItemToStackViewResponseConverter.convert(item, stackInstanceCounts, stackUnhealthyInstanceCounts, sharedClusterName);
                })
                .collect(Collectors.toSet());
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
}
