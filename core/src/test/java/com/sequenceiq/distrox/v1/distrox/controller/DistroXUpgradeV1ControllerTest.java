package com.sequenceiq.distrox.v1.distrox.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeAvailabilityService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DistroXUpgradeV1ControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String CLUSTER_NAME = "clusterName";

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:1234:datahub:1";

    private static final Long WORKSPACE_ID = 1L;

    private static final String ACCOUNT_ID = "1234";

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UpgradeConverter upgradeConverter;

    @Mock
    private DistroXUpgradeAvailabilityService upgradeAvailabilityService;

    @Mock
    private DistroXUpgradeService upgradeService;

    @Mock
    private StackCcmUpgradeService stackCcmUpgradeService;

    @Mock
    private StackService stackService;

    @Mock
    private DistroXUpgradeV1Response distroXUpgradeV1Response;

    @InjectMocks
    private DistroXUpgradeV1Controller underTest;

    @BeforeEach
    public void init() {
        lenient().when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testDryRun() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.TRUE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeAvailabilityService.checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(mock(Stack.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        verify(upgradeAvailabilityService).checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN);
        verifyNoInteractions(upgradeService);
    }

    @Test
    public void testShowAvailableImages() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setShowAvailableImages(UpgradeShowAvailableImages.SHOW);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeAvailabilityService.checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(mock(Stack.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        verify(upgradeAvailabilityService).checkForUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, upgradeV4Request, USER_CRN);
        verifyNoInteractions(upgradeService);
    }

    @Test
    public void testUpgradeCalled() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, false)).thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(mock(Stack.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        verify(upgradeService).triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, false);
    }

    @Test
    public void testUpgradeCalledWithCrn() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, false))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(mock(Stack.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByCrn(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeService).triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, false);
    }

    @Test
    public void testUpgradeOnNonInternalEndpointWhenCodCluster() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        Stack stack = new Stack();
        StackTags stackTags = new StackTags(Map.of(), Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), CodUtil.OPERATIONAL_DB), Map.of());
        stack.setTags(new Json(stackTags));
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByCrn(CLUSTER_NAME, distroxUpgradeRequest)));
        assertEquals("Please note that COD cluster upgrades are supported only through the Operational Database UI or CLI!", exception.getMessage());

        verify(upgradeService, never()).triggerUpgrade(any(), anyLong(), anyString(), any(), anyBoolean());
    }

    @Test
    public void testUpgradePreparationCalled() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, true))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.prepareClusterUpgradeByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        verify(upgradeService).triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, true);
    }

    @Test
    public void testUpgradePreparationCalledWithCrn() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, true))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.prepareClusterUpgradeByCrn(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeService).triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, true);
    }

    @Test
    public void testRollingUpgradeCalledWithCrn() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        distroxUpgradeRequest.setRollingUpgradeEnabled(true);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, false)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, false))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(mock(Stack.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByCrn(CLUSTER_NAME, distroxUpgradeRequest));

        verify(upgradeConverter).convert(distroxUpgradeRequest, USER_CRN, false);
        verify(upgradeService).triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, USER_CRN, upgradeV4Request, false);
    }

    @Test
    public void testInternalRollingUpgradeCalledWithCrn() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        when(upgradeConverter.convert(distroxUpgradeRequest, USER_CRN, true)).thenReturn(upgradeV4Request);
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        when(upgradeService.triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, INTERNAL_CRN, upgradeV4Request, false))
                .thenReturn(upgradeV4Response);
        when(upgradeConverter.convert(upgradeV4Response)).thenReturn(distroXUpgradeV1Response);

        ThreadBasedUserCrnProvider.doAs(INTERNAL_CRN, () -> underTest.upgradeClusterByCrnInternal(CLUSTER_NAME, distroxUpgradeRequest, USER_CRN, true));

        verify(upgradeConverter).convert(distroxUpgradeRequest, USER_CRN, true);
        verify(upgradeService).triggerUpgrade(NameOrCrn.ofCrn(CLUSTER_NAME), WORKSPACE_ID, INTERNAL_CRN, upgradeV4Request, false);
    }

    @Test
    public void testCcmUpgrade() {
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "1");
        StackCcmUpgradeV4Response stackCcmUpgradeV4Response = new StackCcmUpgradeV4Response(CcmUpgradeResponseType.TRIGGERED, flowId, null, "resourceCrn");
        DistroXCcmUpgradeV1Response expected = new DistroXCcmUpgradeV1Response(CcmUpgradeResponseType.TRIGGERED, flowId, null, "resourceCrn");
        when(upgradeConverter.convert(stackCcmUpgradeV4Response)).thenReturn(expected);
        when(stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofCrn(DATAHUB_CRN))).thenReturn(stackCcmUpgradeV4Response);
        DistroXCcmUpgradeV1Response result = underTest.upgradeCcmByCrnInternal(DATAHUB_CRN, USER_CRN);
        assertThat(result.getFlowIdentifier()).isEqualTo(expected.getFlowIdentifier());
        assertThat(result.getReason()).isEqualTo(expected.getReason());
        assertThat(result.getResourceCrn()).isEqualTo(expected.getResourceCrn());
        assertThat(result.getResponseType()).isEqualTo(expected.getResponseType());
    }

}
