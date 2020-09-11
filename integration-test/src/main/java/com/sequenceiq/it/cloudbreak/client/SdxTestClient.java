package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCheckForUpgradeAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCollectCMDiagnosticsAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCollectDiagnosticsAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDescribeAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDescribeInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDetailedDescribeInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxForceDeleteAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxForceDeleteInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxListAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRefreshAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRefreshInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRepairAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRepairInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxStartAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxStopAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxSyncAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxSyncInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxUpgradeAction;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCMDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Service
public class SdxTestClient {

    public Action<SdxTestDto, SdxClient> create() {
        return new SdxCreateAction();
    }

    public Action<SdxInternalTestDto, SdxClient> createInternal() {
        return new SdxCreateInternalAction();
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

    public Action<SdxInternalTestDto, SdxClient> forceDeleteInternal() {
        return new SdxForceDeleteInternalAction();
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

    public Action<SdxTestDto, SdxClient> repair() {
        return new SdxRepairAction();
    }

    public Action<SdxTestDto, SdxClient> checkForUpgrade() {
        return new SdxCheckForUpgradeAction();
    }

    public Action<SdxTestDto, SdxClient> upgrade() {
        return new SdxUpgradeAction();
    }

    public Action<SdxInternalTestDto, SdxClient> repairInternal() {
        return new SdxRepairInternalAction();
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

}
