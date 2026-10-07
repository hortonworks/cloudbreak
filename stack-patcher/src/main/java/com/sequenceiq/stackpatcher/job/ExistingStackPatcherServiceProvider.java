package com.sequenceiq.stackpatcher.job;

import java.util.Collection;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchService;

@Service
public class ExistingStackPatcherServiceProvider {

    @Inject
    private Collection<ExistingStackPatchService<?>> existingStackPatchServices;

    public ExistingStackPatchService<?> provide(String stackPatchTypeName) throws UnknownStackPatchTypeException {
        return existingStackPatchServices.stream()
                .filter(service -> service.getStackPatchType().name().equals(stackPatchTypeName))
                .findFirst()
                .orElseThrow(() -> new UnknownStackPatchTypeException("Could not find stack patcher service for type " + stackPatchTypeName));
    }

    public ExistingStackPatchService<?> provide(StackPatchType stackPatchType) throws UnknownStackPatchTypeException {
        return provide(stackPatchType.name());
    }
}
