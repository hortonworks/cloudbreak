package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.common.model.OsType;

@Component
public class OsChangeUpgradeCondition {

    private static final List<Pair<OsType, OsType>> ALLOWED_OS_CHANGES = List.of(
            Pair.of(CENTOS7, RHEL8),
            Pair.of(RHEL8, RHEL9)
    );

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    public Optional<OsType> getPreviousOs(OsType nextMajorOs) {
        return ALLOWED_OS_CHANGES
                .stream()
                .filter(osChange -> osChange.getRight().equals(nextMajorOs))
                .map(Pair::getLeft)
                .findFirst();
    }

    public boolean isNextMajorOsImage(Long stackId, Image imageCandidate) {
        if (CENTOS7.equals(OsType.getByOsTypeStringWithCentos7Fallback(imageCandidate.getOsType()))) {
            return false;
        }
        Set<OsType> osUsedByInstances = currentImageUsageCondition.getOSUsedByInstances(stackId);
        return isNextMajorOsImage(osUsedByInstances, imageCandidate);
    }

    public boolean isNextMajorOsImage(Set<OsType> osUsedByInstances, Image imageCandidate) {
        return ALLOWED_OS_CHANGES
                .stream()
                .filter(osChange -> allUse(osChange.getLeft(), osUsedByInstances) && imageHasOs(osChange.getRight(), imageCandidate))
                .map(Pair::getRight)
                .findFirst()
                .isPresent();
    }

    private boolean allUse(OsType osType, Set<OsType> usedOperatingSystems) {
        return usedOperatingSystems.size() == 1 && usedOperatingSystems.contains(osType);
    }

    private boolean imageHasOs(OsType osType, Image image) {
        return osType.matches(image.getOs(), image.getOsType());
    }
}
