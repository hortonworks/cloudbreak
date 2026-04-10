package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.findLastStatusIndexFromListByMultipleStatuses;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;

@Service
public class UpgradeReinitiateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeReinitiateService.class);

    @Inject
    private StackStatusService stackStatusService;

    public UpgradeReinitiableV4Response checkClusterUpgradeReinitiable(Long stackId) {
        List<DetailedStackStatus> detailedStackStatusList = stackStatusService.findAllStackStatusesById(stackId).stream()
                .map(StackStatus::getDetailedStackStatus)
                .toList();
        int lastUpgradeSuccessIndex = findLastStatusIndexFromListByMultipleStatuses(detailedStackStatusList, DetailedStackStatus.getUpgradeSuccessStatuses());
        int lastUpgradeFailureIndex = findLastStatusIndexFromListByMultipleStatuses(detailedStackStatusList, DetailedStackStatus.getUpgradeFailureStatuses());
        LOGGER.info("Last upgrade success index: {}, last upgrade failure index: {}", lastUpgradeSuccessIndex, lastUpgradeFailureIndex);

        int lastRelevantStatusIndex = Math.max(lastUpgradeSuccessIndex, lastUpgradeFailureIndex);
        if (lastRelevantStatusIndex == -1) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "There were no upgrades for this cluster, therefore upgrade reinitiation is not needed."
            );
        } else if (lastRelevantStatusIndex == lastUpgradeSuccessIndex) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The last upgrade for this cluster finished successfully, therefore upgrade reinitiation is not needed."
            );
        } else if (lastRelevantStatusIndex == lastUpgradeFailureIndex) {
            return new UpgradeReinitiableV4Response(
                    UpgradeReinitiateStatus.REINITIABLE,
                    "The last upgrade for this cluster finished with a failure, therefore the cluster is eligible for upgrade reinitiation."
            );
        } else {
            return new UpgradeReinitiableV4Response(UpgradeReinitiateStatus.NON_REINITIABLE);
        }
    }
}
