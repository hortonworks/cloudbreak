package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPGRADE_CCM_IN_PROGRESS;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Service
public class UpgradeCcmService {

    @Inject
    private StackUpdater stackUpdater;

    public void checkPrerequsities(Long stackId) {
        return;
    }

    public void checkPrerequisitesState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_IN_PROGRESS;
        String statusReason = "Upgrade CCM in progress";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void finishedState(Long stackId) {
        DetailedStackStatus detailedStatus = AVAILABLE;
        String statusReason = "Upgrade CCM completed";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void failedState(Long stackId) {
        DetailedStackStatus detailedStatus = UPGRADE_CCM_FAILED;
        String statusReason = "Upgrade CCM failed";
        stackUpdater.updateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void changeTunnelState(Long stackId) {

    }

    public void changeTunnel(Long stackId) {

    }

    public void pushSaltStatesState(Long stackId) {

    }

    public void pushSaltStates(Long stackId) {

    }

    public void upgradeState(Long stackId) {

    }

    public void upgrade(Long stackId) {

    }

    public void reconfigureState(Long stackId) {

    }

    public void reconfigure(Long stackId) {

    }

    public void registerCcmState(Long stackId) {

    }

    public void registerCcm(Long stackId) {

    }

    public void healthCheckState(Long stackId) {

    }

    public void healthCheck(Long stackId) {

    }

    public void removeMinaState(Long stackId) {

    }

    public void removeMina(Long stackId) {

    }
}
