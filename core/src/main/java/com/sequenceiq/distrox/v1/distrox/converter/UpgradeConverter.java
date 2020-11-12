package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Response;

@Component
public class UpgradeConverter {

    public UpgradeV4Request convert(DistroxUpgradeV1Request source) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(source.getImageId());
        request.setRuntime(source.getRuntime());
        request.setDryRun(source.getDryRun());
        request.setLockComponents(source.getLockComponents());
        Optional.ofNullable(source.getShowAvailableImages())
                .ifPresent(value -> request.setShowAvailableImages(UpgradeShowAvailableImages.valueOf(value.name())));
        request.setReplaceVms(DistroxUpgradeReplaceVms.ENABLED == source.getReplaceVms());
        return request;
    }

    public DistroxUpgradeV1Response convert(UpgradeV4Response source) {
        return new DistroxUpgradeV1Response(source.getCurrent(), source.getUpgradeCandidates(), source.getReason(), source.getFlowIdentifier());
    }
}
