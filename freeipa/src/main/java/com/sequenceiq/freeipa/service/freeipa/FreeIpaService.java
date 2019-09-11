package com.sequenceiq.freeipa.service.freeipa;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaServerRequestToFreeIpaConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.FreeIpaRepository;

@Service
public class FreeIpaService implements ResourceIdProvider {

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
}
