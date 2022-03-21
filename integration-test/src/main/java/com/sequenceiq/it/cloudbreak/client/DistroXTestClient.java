package com.sequenceiq.it.cloudbreak.client;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.CheckVariant;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXBlueprintRequestAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXChangeImageCatalogAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXCreateAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXCreateInternalAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXForceDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXGetAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXRepairInstancesAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXInternalGetAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXRefreshAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXRemoveInstancesAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXRepairAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXScaleAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXScaleStartInstancesAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXScaleStopInstancesAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXShowBlueprintAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXStartAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXStopAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXUpgradeAction;
import com.sequenceiq.it.cloudbreak.action.v4.util.RenewDistroXCertificateAction;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RenewDistroXCertificateTestDto;

@Service
public class DistroXTestClient {

    public Action<DistroXTestDto, CloudbreakClient> create() {
        return new DistroXCreateAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> createInternal() {
        return new DistroXCreateInternalAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> delete() {
        return new DistroXDeleteAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> forceDelete() {
        return new DistroXForceDeleteAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> blueprintRequest() {
        return new DistroXBlueprintRequestAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> get() {
        return new DistroXGetAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> refresh() {
        return new DistroXRefreshAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> getInternal() {
        return new DistroXInternalGetAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> stop() {
        return new DistroXStopAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> start() {
        return new DistroXStartAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> scale(String hostGroup, Integer count) {
        return new DistroXScaleAction(hostGroup, count, AdjustmentType.EXACT, null);
    }

    public Action<DistroXTestDto, CloudbreakClient> scale(String hostGroup, Integer count, AdjustmentType adjustmentType, Long threshold) {
        return new DistroXScaleAction(hostGroup, count, adjustmentType, threshold);
    }

    public Action<DistroXTestDto, CloudbreakClient> scaleStopInstances() {
        return new DistroXScaleStopInstancesAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> scaleStartInstances(String hostGroup, Integer count) {
        return new DistroXScaleStartInstancesAction(hostGroup, count);
    }

    public Action<DistroXTestDto, CloudbreakClient> postStackForBlueprint() {
        return new DistroXShowBlueprintAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> removeInstances() {
        return new DistroXRemoveInstancesAction();
    }

    public Action<RenewDistroXCertificateTestDto, CloudbreakClient> renewDistroXCertificateV4() {
        return new RenewDistroXCertificateAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> repair(HostGroupType... hostGroupTypes) {
        return new DistroXRepairAction(List.of(hostGroupTypes));
    }

    public Action<DistroXTestDto, CloudbreakClient> repairInstances() {
        return new DistroXRepairInstancesAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> upgrade() {
        return new DistroXUpgradeAction();
    }

    public Action<DistroXChangeImageCatalogTestDto, CloudbreakClient> changeImageCatalog() {
        return new DistroXChangeImageCatalogAction();
    }

    public Action<DistroXTestDto, CloudbreakClient> checkVariant(String variant) {
        return new CheckVariant(variant);
    }
}
