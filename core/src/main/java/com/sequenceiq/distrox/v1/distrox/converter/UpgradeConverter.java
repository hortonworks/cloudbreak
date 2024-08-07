package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;

@Component
public class UpgradeConverter {

    @Inject
    private AccountIdService accountIdService;

    public UpgradeV4Request convert(DistroXUpgradeV1Request source, String initiatorUserCrn, boolean skipValidations) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(source.getImageId());
        request.setRuntime(source.getRuntime());
        request.setDryRun(source.getDryRun());
        request.setLockComponents(source.getLockComponents());
        request.setInternalUpgradeSettings(new InternalUpgradeSettings(skipValidations, Boolean.TRUE.equals(source.getRollingUpgradeEnabled())));
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

    public DistroXCcmUpgradeV1Response convert(StackCcmUpgradeV4Response source) {
        return new DistroXCcmUpgradeV1Response(source.getResponseType(), source.getFlowIdentifier(), source.getReason(), source.getResourceCrn());
    }

    private String getAccountId(String userCrn) {
        return accountIdService.getAccountIdFromUserCrn(userCrn);
    }
}
