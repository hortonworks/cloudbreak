package com.sequenceiq.freeipa.service.freeipa;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaServerRequestToFreeIpaConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.FreeIpaRepository;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class FreeIpaService implements ResourceIdProvider, PayloadContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Inject
    private FreeIpaRepository repository;

    @Inject
    private FreeIpaServerRequestToFreeIpaConverter freeIpaConverter;

    public FreeIpa save(FreeIpa freeIpa) {
        return repository.save(freeIpa);
    }

    public FreeIpa findByStack(Stack stack) {
        return repository.getByStack(stack).orElseThrow(() -> new NotFoundException(String.format("FreeIpa not found for Stack [%s]", stack.getId())));
    }

    public FreeIpa findByStackId(Long stackId) {
        return repository.getByStackId(stackId).orElseThrow(() -> new NotFoundException(String.format("FreeIpa not found for Stack [%s]", stackId)));
    }

    public FreeIpa create(Stack stack, FreeIpaServerRequest request) {
        FreeIpa freeIpa = freeIpaConverter.convert(request);
        freeIpa.setStack(stack);
        return save(freeIpa);
    }

    public List<FreeIpa> getAllByAccountId(String accountId) {
        return repository.findByAccountId(accountId);
    }

    @Override
    public Long getResourceIdByResourceCrn(String environmentCrn) {
        return stackService.getByEnvironmentCrnAndAccountId(environmentCrn, crnService.getCurrentAccountId()).getId();
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        try {
            Stack stack = stackService.getStackById(resourceId);
            return PayloadContext.create(stack.getResourceCrn(), stack.getEnvironmentCrn(), stack.getCloudPlatform());
        } catch (NotFoundException notFoundException) {
            // skip
        }
        return null;
    }

    public List<FreeIpa> getAllByIds(List<Long> ids) {
        return repository.findAllByIds(ids);
    }

    public List<ResourceWithId> getAllAsAuthorizationResources(String accountId) {
        return repository.findAllAsAuthorizationResources(accountId);
    }
}
