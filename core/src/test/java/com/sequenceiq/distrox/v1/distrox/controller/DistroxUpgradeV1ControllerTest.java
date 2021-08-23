package com.sequenceiq.distrox.v1.distrox.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;

@ExtendWith(MockitoExtension.class)
class DistroxUpgradeV1ControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String CLUSTER_NAME = "clusterName";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UpgradeConverter upgradeConverter;

    @Mock
    private DistroXUpgradeAvailabilityService upgradeAvailabilityService;

    @Mock
    private DistroXUpgradeService upgradeService;

    @InjectMocks
    private DistroXUpgradeV1Controller underTest;

    @BeforeEach
    public void init() {
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testDryRun() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.TRUE);
        when(upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId("1234")).thenReturn(true);
        when(upgradeConverter.convert(distroxUpgradeRequest, new InternalUpgradeSettings(false, true))).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeAvailabilityService.checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroXUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeAvailabilityService).checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN);
        verifyNoInteractions(upgradeService);
    }

    @Test
    public void testShowAvailableImages() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setShowAvailableImages(UpgradeShowAvailableImages.SHOW);
        when(upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId("1234")).thenReturn(true);
        when(upgradeConverter.convert(distroxUpgradeRequest, new InternalUpgradeSettings(false, true))).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeAvailabilityService.checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroXUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeAvailabilityService).checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN);
        verifyNoInteractions(upgradeService);
    }

    @Test
    public void testUpgradeCalled() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId("1234")).thenReturn(true);
        when(upgradeConverter.convert(distroxUpgradeRequest, new InternalUpgradeSettings(false, true))).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroXUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeService).triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request);
    }

    @Test
    public void testUpgradeCalledWithCrn() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeAvailabilityService.isRuntimeUpgradeEnabledByAccountId("1234")).thenReturn(true);
        when(upgradeConverter.convert(distroxUpgradeRequest, new InternalUpgradeSettings(false, true))).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroXUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByCrn(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeService).triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request);
    }

}