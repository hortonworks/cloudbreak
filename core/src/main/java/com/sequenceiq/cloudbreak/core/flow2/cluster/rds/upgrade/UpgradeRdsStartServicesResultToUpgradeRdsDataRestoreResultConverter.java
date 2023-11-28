package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;
import com.sequenceiq.flow.core.PayloadConverter;

// TODO This is for backward compatibility reason, can be removed in CB-24447
public class UpgradeRdsStartServicesResultToUpgradeRdsDataRestoreResultConverter implements PayloadConverter<UpgradeRdsDataRestoreResult> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return UpgradeRdsStartServicesResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpgradeRdsDataRestoreResult convert(Object payload) {
        UpgradeRdsStartServicesResult source = (UpgradeRdsStartServicesResult) payload;
        return new UpgradeRdsDataRestoreResult(source.getResourceId(), source.getVersion());
    }
}
