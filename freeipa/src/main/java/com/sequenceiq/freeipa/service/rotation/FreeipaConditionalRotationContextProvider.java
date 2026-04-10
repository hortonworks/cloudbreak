package com.sequenceiq.freeipa.service.rotation;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.common.ConditionalRotationContextProvider;
import com.sequenceiq.freeipa.entity.Stack;

public abstract class FreeipaConditionalRotationContextProvider implements ConditionalRotationContextProvider<Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaConditionalRotationContextProvider.class);

    @Override
    public boolean isApplicable(Stack entity) {
        try {
            return getConditionalRotationFunction().apply(entity);
        } catch (Exception e) {
            LOGGER.warn(String.format("Couldn't decide if rotation for resource %s is applicable, thus allowing it, reason: ", entity.getResourceCrn()), e);
            return true;
        }
    }

    protected abstract Function<Stack, Boolean> getConditionalRotationFunction();
}
