package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxAutotlsCertRotationAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxBackupAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxBackupInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxChangeImageCatalogAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCheckForUpgradeAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCollectCMDiagnosticsAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCollectDiagnosticsAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateCustomAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteCustomAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDescribeAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDescribeInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDetailedDescribeInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxEnableRangerRazAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxForceDeleteAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxForceDeleteCustomAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxForceDeleteInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxGetAuditsAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxGetDatalakeEventsZipAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxListAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRecoveryAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRefreshAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRefreshCustomAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRefreshInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRepairAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRepairInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxResizeAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRestoreAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRestoreInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxStartAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxStatusAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxStopAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxSyncAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxSyncInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxUpgradeAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.RenewDatalakeCertificateAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.SdxRetryAction;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCMDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RenewDatalakeCertificateTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.SdxEventTestDto;

@Service
public class SdxTestClient {

    public Action<SdxTestDto, SdxClient> create() {
        return new SdxCreateAction();
    }

    public Action<SdxInternalTestDto, SdxClient> createInternal() {
        return new SdxCreateInternalAction();
    }

    public Action<SdxCustomTestDto, SdxClient> createCustom() {
        return new SdxCreateCustomAction();
    }

    public Action<SdxTestDto, SdxClient> delete() {
        return new SdxDeleteAction();
    }

    public Action<SdxTestDto, SdxClient> forceDelete() {
        return new SdxForceDeleteAction();
    }

    public Action<SdxInternalTestDto, SdxClient> deleteInternal() {
        return new SdxDeleteInternalAction();
    }

    public Action<SdxCustomTestDto, SdxClient> deleteCustom() {
        return new SdxDeleteCustomAction();
    }

    public Action<SdxInternalTestDto, SdxClient> forceDeleteInternal() {
        return new SdxForceDeleteInternalAction();
    }

    public Action<SdxCustomTestDto, SdxClient> forceDeleteCustom() {
        return new SdxForceDeleteCustomAction();
    }

    public Action<SdxTestDto, SdxClient> describe() {
        return new SdxDescribeAction();
    }

    public Action<SdxInternalTestDto, SdxClient> describeInternal() {
        return new SdxDescribeInternalAction();
    }

    public Action<SdxInternalTestDto, SdxClient> detailedDescribeInternal() {
        return new SdxDetailedDescribeInternalAction();
    }

    public Action<SdxTestDto, SdxClient> list() {
        return new SdxListAction();
    }

    public Action<SdxTestDto, SdxClient> sync() {
        return new SdxSyncAction();
    }

    public Action<SdxTestDto, SdxClient> refresh() {
        return new SdxRefreshAction();
    }

    public Action<SdxInternalTestDto, SdxClient> refreshInternal() {
        return new SdxRefreshInternalAction();
    }

    public Action<SdxCustomTestDto, SdxClient> refreshCustom() {
        return new SdxRefreshCustomAction();
    }

    public Action<SdxTestDto, SdxClient> repair(String... hostGroups) {
        return new SdxRepairAction(hostGroups);
    }

    public Action<SdxTestDto, SdxClient> checkForUpgrade() {
        return new SdxCheckForUpgradeAction();
    }

    public Action<SdxTestDto, SdxClient> checkStatus(String sdxName) {
        return new SdxStatusAction(sdxName);
    }

    public Action<SdxTestDto, SdxClient> upgrade() {
        return new SdxUpgradeAction();
    }

    public Action<SdxTestDto, SdxClient> recover() {
        return new SdxRecoveryAction();
    }

    public Action<SdxInternalTestDto, SdxClient> resize() {
        return new SdxResizeAction();
    }

    public Action<SdxTestDto, SdxClient> rotateAutotlsCertificates() {
        return new SdxAutotlsCertRotationAction();
    }

    public Action<SdxInternalTestDto, SdxClient> repairInternal(String... hostGroups) {
        return new SdxRepairInternalAction(hostGroups);
    }

    public Action<SdxInternalTestDto, SdxClient> syncInternal() {
        return new SdxSyncInternalAction();
    }

    public Action<SdxInternalTestDto, SdxClient> startInternal() {
        return new SdxStartAction();
    }

    public Action<SdxInternalTestDto, SdxClient> stopInternal() {
        return new SdxStopAction();
    }

    public Action<SdxDiagnosticsTestDto, SdxClient> collectDiagnostics() {
        return new SdxCollectDiagnosticsAction();
    }

    public Action<SdxCMDiagnosticsTestDto, SdxClient> collectCMDiagnostics() {
        return new SdxCollectCMDiagnosticsAction();
    }

    public Action<RenewDatalakeCertificateTestDto, SdxClient> renewDatalakeCertificateV4() {
        return new RenewDatalakeCertificateAction();
    }

    public Action<SdxInternalTestDto, SdxClient> retry() {
        return new SdxRetryAction();
    }

    public Action<SdxTestDto, SdxClient> backup(String backupLocation, String backupName) {
        return new SdxBackupAction(backupLocation, backupName);
    }

    public Action<SdxInternalTestDto, SdxClient> backupInternal(String backupLocation, String backupName) {
        return new SdxBackupInternalAction(backupLocation, backupName);
    }

    public Action<SdxTestDto, SdxClient> restore(String backupId, String backupLocation) {
        return new SdxRestoreAction(backupId, backupLocation);
    }

    public Action<SdxChangeImageCatalogTestDto, SdxClient> changeImageCatalog() {
        return new SdxChangeImageCatalogAction();
    }

    public Action<SdxTestDto, SdxClient> enableRangerRaz() {
        return new SdxEnableRangerRazAction();
    }

    public Action<SdxInternalTestDto, SdxClient> restoreInternal(String backupId, String backupLocation) {
        return new SdxRestoreInternalAction(backupId, backupLocation);
    }

    public Action<SdxEventTestDto, SdxClient> getAuditEvents() {
        return new SdxGetAuditsAction();
    }

    public Action<SdxEventTestDto, SdxClient> getDatalakeEventsZip() {
        return new SdxGetDatalakeEventsZipAction();
    }
}
