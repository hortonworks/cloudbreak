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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.ComponentLocker;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroxUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroxUpgradeService;

@ExtendWith(MockitoExtension.class)
class DistroxUpgradeV1ControllerTest {

    private static final String USER_CRN = "userCrn";

    private static final String CLUSTER_NAME = "clusterName";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UpgradeConverter upgradeConverter;

    @Mock
    private DistroxUpgradeAvailabilityService upgradeAvailabilityService;

    @Mock
    private DistroxUpgradeService upgradeService;

    @Mock
    private ComponentLocker componentLocker;

    @InjectMocks
    private DistroxUpgradeV1Controller underTest;

    @BeforeEach
    public void init() {
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testDryRun() {
        DistroxUpgradeV1Request distroxUpgradeRequest = new DistroxUpgradeV1Request();
        when(componentLocker.lockComponentsIfRuntimeUpgradeIsDisabled(distroxUpgradeRequest, USER_CRN,  CLUSTER_NAME)).thenReturn(distroxUpgradeRequest);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.TRUE);
        when(upgradeConverter.convert(distroxUpgradeRequest)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeAvailabilityService.checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroxUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeAvailabilityService).checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN);
        verifyNoInteractions(upgradeService);
    }

    @Test
    public void testShowAvailableImages() {
        DistroxUpgradeV1Request distroxUpgradeRequest = new DistroxUpgradeV1Request();
        when(componentLocker.lockComponentsIfRuntimeUpgradeIsDisabled(distroxUpgradeRequest, USER_CRN,  CLUSTER_NAME)).thenReturn(distroxUpgradeRequest);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setShowAvailableImages(UpgradeShowAvailableImages.SHOW);
        when(upgradeConverter.convert(distroxUpgradeRequest)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeAvailabilityService.checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroxUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeAvailabilityService).checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN);
        verifyNoInteractions(upgradeService);
    }

    @Test
    public void testUpgradeCalled() {
        DistroxUpgradeV1Request distroxUpgradeRequest = new DistroxUpgradeV1Request();
        when(componentLocker.lockComponentsIfRuntimeUpgradeIsDisabled(distroxUpgradeRequest, USER_CRN,  CLUSTER_NAME)).thenReturn(distroxUpgradeRequest);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroxUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeService).triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request);
        verifyNoInteractions(upgradeAvailabilityService);
    }

    @Test
    public void testUpgradeCalledWithCrn() {
        DistroxUpgradeV1Request distroxUpgradeRequest = new DistroxUpgradeV1Request();
        when(componentLocker.lockComponentsIfRuntimeUpgradeIsDisabled(distroxUpgradeRequest, USER_CRN,  CLUSTER_NAME)).thenReturn(distroxUpgradeRequest);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(new DistroxUpgradeV1Response());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByCrn(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeService).triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request);
        verifyNoInteractions(upgradeAvailabilityService);
    }

}