package com.sequenceiq.cloudbreak.job.stackpatcher;

import java.util.Collection;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.converter.StackPatchTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;

@Service
public class ExistingStackPatcherServiceProvider {

    @Inject
    private Collection<ExistingStackPatchService> existingStackPatchServices;

    @Inject
    private StackPatchTypeConverter stackPatchTypeConverter;

    public ExistingStackPatchService provide(String stackPatchTypeName) throws UnknownStackPatchTypeException {
        StackPatchType stackPatchType = stackPatchTypeConverter.convertToEntityAttribute(stackPatchTypeName);
        if (stackPatchType == null || StackPatchType.UNKNOWN.equals(stackPatchType)) {
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
