package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;

@Component
public class UpgradeConverter {
    public UpgradeV4Request convert(DistroXUpgradeV1Request source, InternalUpgradeSettings internalUpgradeSettings) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(source.getImageId());
        request.setRuntime(source.getRuntime());
        request.setDryRun(source.getDryRun());
        request.setLockComponents(source.getLockComponents());
        request.setInternalUpgradeSettings(internalUpgradeSettings);
        Optional.ofNullable(source.getShowAvailableImages())
                .ifPresent(value -> request.setShowAvailableImages(UpgradeShowAvailableImages.valueOf(value.name())));
        request.setReplaceVms(convertReplaceVms(source));
        return request;
    }

    private Boolean convertReplaceVms(DistroXUpgradeV1Request source) {
        Boolean result = null;
        if (source.getReplaceVms() != null) {
            result = DistroXUpgradeReplaceVms.ENABLED == source.getReplaceVms();
        }
        return result;
    }

    public DistroXUpgradeV1Response convert(UpgradeV4Response source) {
        return new DistroXUpgradeV1Response(source.getCurrent(), source.getUpgradeCandidates(), source.getReason(), source.getFlowIdentifier());
    }
}
