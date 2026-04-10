package com.sequenceiq.datalake.service.rotation.context.provider;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.common.ConditionalRotationContextProvider;
import com.sequenceiq.datalake.entity.SdxCluster;

public abstract class DatalakeConditionalRotationContextProvider extends AbstractDatalakeRotationContextProvider
        implements ConditionalRotationContextProvider<SdxCluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeConditionalRotationContextProvider.class);

    @Override
    public boolean isApplicable(SdxCluster entity) {
        try {
            return getConditionalRotationFunction().apply(entity);
        } catch (Exception e) {
            LOGGER.warn(String.format("Couldn't decide if rotation for resource %s is applicable, thus allowing it, reason: ", entity.getCrn()), e);
            return true;
        }
    }

    protected abstract Function<SdxCluster, Boolean> getConditionalRotationFunction();
}
