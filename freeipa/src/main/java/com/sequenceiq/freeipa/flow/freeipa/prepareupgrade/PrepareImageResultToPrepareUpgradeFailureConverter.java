package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;

public class PrepareImageResultToPrepareUpgradeFailureConverter implements PayloadConverter<PrepareUpgradeFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return PrepareImageResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public PrepareUpgradeFailureEvent convert(Object payload) {
        PrepareImageResult prepareImageResult = (PrepareImageResult) payload;
        return new PrepareUpgradeFailureEvent(prepareImageResult.getResourceId(), FailureType.VALIDATION, prepareImageResult.getErrorDetails());
    }
}
