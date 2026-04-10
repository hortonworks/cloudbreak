package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.ConditionalRotationContextProvider;

public abstract class CloudbreakConditionalRotationContextProvider implements ConditionalRotationContextProvider<StackDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakConditionalRotationContextProvider.class);

    @Override
    public boolean isApplicable(StackDto entity) {
        try {
            return getConditionalRotationFunction().apply(entity);
        } catch (Exception e) {
            LOGGER.warn(String.format("Couldn't decide if rotation for resource %s is applicable, thus allowing it, reason: ", entity.getResourceCrn()), e);
            return true;
        }
    }

    protected abstract Function<StackDto, Boolean> getConditionalRotationFunction();
}
