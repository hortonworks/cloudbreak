package com.sequenceiq.freeipa.job.stackpatcher;

import java.util.Collection;

import jakarta.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.StackPatchType;
import com.sequenceiq.freeipa.service.stackpatch.ExistingStackPatchService;

@Service
public class ExistingStackPatcherServiceProvider {

    @Inject
    private Collection<ExistingStackPatchService> existingStackPatchServices;

    public ExistingStackPatchService provide(String stackPatchTypeName) throws UnknownStackPatchTypeException {
        StackPatchType stackPatchType = EnumUtils.getEnum(StackPatchType.class, stackPatchTypeName, StackPatchType.UNKNOWN);
        if (StackPatchType.UNKNOWN.equals(stackPatchType)) {
            throw new UnknownStackPatchTypeException(String.format("Stack patch type %s is unknown", stackPatchTypeName));
        }
        return provide(stackPatchType);
    }

    public ExistingStackPatchService provide(StackPatchType stackPatchType) throws UnknownStackPatchTypeException {
        return existingStackPatchServices.stream()
                .filter(existingStackPatchService -> stackPatchType.equals(existingStackPatchService.getStackPatchType()))
                .findFirst()
                .orElseThrow(() -> new UnknownStackPatchTypeException("No stack patcher implementation found for type " + stackPatchType));
    }
}
