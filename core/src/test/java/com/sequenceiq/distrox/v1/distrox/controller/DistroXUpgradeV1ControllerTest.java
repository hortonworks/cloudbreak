package com.sequenceiq.distrox.v1.distrox.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXDatabaseUpgradeStatus;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXRdsUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.DistroXUpgradeReinitiableV1Response;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.DistroXUpgradeService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.rds.DistroXRdsUpgradeService;
import com.sequenceiq.distrox.v1.distrox.service.upgrade.rds.DistroXRdsUpgradeStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DistroXUpgradeV1ControllerTest {

    private static final Long STACK_ID = 1L;

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
    private DistroXRdsUpgradeService rdsUpgradeService;

    @Mock
    private DistroXRdsUpgradeStatusService rdsUpgradeStatusService;

    @Mock
    private StackCcmUpgradeService stackCcmUpgradeService;

    @Mock
    private StackService stackService;

    @Mock
    private DistroXUpgradeService distroXUpgradeService;

    @Mock
    private DistroXUpgradeV1Response distroXUpgradeV1Response;

    @InjectMocks
    private DistroXUpgradeV1Controller underTest;

    @BeforeEach
    void init() {
        lenient().when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    void testUpgradeRdsByName() {
        DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest = new DistroXRdsUpgradeV1Request();
        DistroXRdsUpgradeV1Response expected = new DistroXRdsUpgradeV1Response();
        when(rdsUpgradeService.triggerUpgrade(NameOrCrn.ofName(CLUSTER_NAME), distroxRdsUpgradeRequest)).thenReturn(expected);

        DistroXRdsUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.upgradeRdsByName(CLUSTER_NAME, distroxRdsUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        assertEquals(expected, result);
    }

    @Test
    void testUpgradeRdsByCrn() {
        DistroXRdsUpgradeV1Request distroxRdsUpgradeRequest = new DistroXRdsUpgradeV1Request();
        DistroXRdsUpgradeV1Response expected = new DistroXRdsUpgradeV1Response();
        when(rdsUpgradeService.triggerUpgrade(NameOrCrn.ofCrn(DATAHUB_CRN), distroxRdsUpgradeRequest)).thenReturn(expected);

        DistroXRdsUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.upgradeRdsByCrn(DATAHUB_CRN, distroxRdsUpgradeRequest));

        assertEquals(expected, result);
    }

    @EnumSource(DistroXUpgradeShowAvailableImages.class)
    @ParameterizedTest
    void testUpgradeClusterByName(DistroXUpgradeShowAvailableImages upgradeShowAvailableImages) {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        distroxUpgradeRequest.setShowAvailableImages(upgradeShowAvailableImages);
        DistroXUpgradeV1Response expected = mock();
        when(distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, NameOrCrn.ofName(CLUSTER_NAME), false, WORKSPACE_ID)).thenReturn(expected);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        verify(distroXUpgradeService)
                .validateCodCluster(NameOrCrn.ofName(CLUSTER_NAME), upgradeShowAvailableImages, WORKSPACE_ID);
        assertEquals(expected, result);
    }

    @EnumSource(DistroXUpgradeShowAvailableImages.class)
    @ParameterizedTest
    void testUpgradeClusterByCrn(DistroXUpgradeShowAvailableImages upgradeShowAvailableImages) {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        distroxUpgradeRequest.setShowAvailableImages(upgradeShowAvailableImages);
        DistroXUpgradeV1Response expected = mock();
        when(distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, NameOrCrn.ofCrn(DATAHUB_CRN), false, WORKSPACE_ID)).thenReturn(expected);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByCrn(DATAHUB_CRN, distroxUpgradeRequest));

        verify(distroXUpgradeService)
                .validateCodCluster(NameOrCrn.ofCrn(DATAHUB_CRN), upgradeShowAvailableImages, WORKSPACE_ID);
        assertEquals(expected, result);
    }

    @Test
    void testUpgradeOnNonInternalEndpointWhenCodCluster() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        distroxUpgradeRequest.setShowAvailableImages(DistroXUpgradeShowAvailableImages.DO_NOT_SHOW);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(Boolean.FALSE);
        BadRequestException expected =
                new BadRequestException("Please note that COD cluster upgrades are supported only through the Operational Database UI or CLI!");
        doThrow(expected)
                .when(distroXUpgradeService).validateCodCluster(NameOrCrn.ofName(CLUSTER_NAME), DistroXUpgradeShowAvailableImages.DO_NOT_SHOW, WORKSPACE_ID);

        BadRequestException result = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, distroxUpgradeRequest)));
        assertEquals(expected, result);

        verify(distroXUpgradeService, never()).upgradeCluster(any(DistroXUpgradeV1Request.class), any(NameOrCrn.class), anyBoolean(), anyLong());
    }

    @Test
    void testPrepareClusterUpgradeByName() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        DistroXUpgradeV1Response expected = mock();
        when(distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, NameOrCrn.ofName(CLUSTER_NAME), true, WORKSPACE_ID)).thenReturn(expected);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.prepareClusterUpgradeByName(CLUSTER_NAME, distroxUpgradeRequest));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        assertEquals(expected, result);
    }

    @Test
    void testPrepareClusterUpgradeByCrn() {
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        DistroXUpgradeV1Response expected = mock();
        when(distroXUpgradeService.upgradeCluster(distroxUpgradeRequest, NameOrCrn.ofCrn(DATAHUB_CRN), true, WORKSPACE_ID)).thenReturn(expected);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.prepareClusterUpgradeByCrn(DATAHUB_CRN, distroxUpgradeRequest));

        assertEquals(expected, result);
    }

    @Test
    void testGetClusterUpgradeReinitiableByName() {
        DistroXUpgradeReinitiableV1Response expected = mock();
        when(distroXUpgradeService.checkClusterUpgradeReinitiable(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID)).thenReturn(expected);

        DistroXUpgradeReinitiableV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.getClusterUpgradeReinitiableByName(CLUSTER_NAME));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        assertEquals(expected, result);
    }

    @Test
    void testGetClusterUpgradeReinitiableByCrn() {
        DistroXUpgradeReinitiableV1Response expected = mock();
        when(distroXUpgradeService.checkClusterUpgradeReinitiable(NameOrCrn.ofCrn(DATAHUB_CRN), WORKSPACE_ID)).thenReturn(expected);

        DistroXUpgradeReinitiableV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.getClusterUpgradeReinitiableByCrn(DATAHUB_CRN));

        assertEquals(expected, result);
    }

    @Test
    void testReinitiateClusterUpgradeByName() {
        when(distroXUpgradeService.reinitiateClusterUpgrade(NameOrCrn.ofName(CLUSTER_NAME), WORKSPACE_ID)).thenReturn(distroXUpgradeV1Response);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.reinitiateClusterUpgradeByName(CLUSTER_NAME));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        assertEquals(distroXUpgradeV1Response, result);
    }

    @Test
    void testReinitiateClusterUpgradeByCrn() {
        when(distroXUpgradeService.reinitiateClusterUpgrade(NameOrCrn.ofCrn(DATAHUB_CRN), WORKSPACE_ID)).thenReturn(distroXUpgradeV1Response);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.reinitiateClusterUpgradeByCrn(DATAHUB_CRN));

        assertEquals(distroXUpgradeV1Response, result);
    }

    static Stream<Arguments> testUpgradeClusterByXInternalArguments() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(true, true)
        );
    }

    @MethodSource("testUpgradeClusterByXInternalArguments")
    @ParameterizedTest
    void testUpgradeClusterByNameInternal(boolean rollingUpgradeParam, boolean rollingUpgradeFromRequest) {
        boolean expectedRollingUpgradeParam = rollingUpgradeParam || rollingUpgradeFromRequest;
        ArgumentCaptor<DistroXUpgradeV1Request> captor = ArgumentCaptor.forClass(DistroXUpgradeV1Request.class);
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        distroxUpgradeRequest.setRollingUpgradeEnabled(rollingUpgradeFromRequest);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        when(upgradeConverter.convert(captor.capture(), eq(true))).thenReturn(upgradeV4Request);
        when(distroXUpgradeService.upgradeCluster(upgradeV4Request, NameOrCrn.ofName(CLUSTER_NAME), false, WORKSPACE_ID)).thenReturn(distroXUpgradeV1Response);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.upgradeClusterByNameInternal(CLUSTER_NAME, distroxUpgradeRequest, USER_CRN, rollingUpgradeParam));

        verify(stackService).checkLiveStackExistenceByName(CLUSTER_NAME, ACCOUNT_ID, StackType.WORKLOAD);
        assertEquals(expectedRollingUpgradeParam, captor.getValue().getRollingUpgradeEnabled());
        assertEquals(distroXUpgradeV1Response, result);
    }

    @MethodSource("testUpgradeClusterByXInternalArguments")
    @ParameterizedTest
    void testUpgradeClusterByCrnInternal(boolean rollingUpgradeParam, boolean rollingUpgradeFromRequest) {
        boolean expectedRollingUpgradeParam = rollingUpgradeParam || rollingUpgradeFromRequest;
        ArgumentCaptor<DistroXUpgradeV1Request> captor = ArgumentCaptor.forClass(DistroXUpgradeV1Request.class);
        DistroXUpgradeV1Request distroxUpgradeRequest = new DistroXUpgradeV1Request();
        distroxUpgradeRequest.setRollingUpgradeEnabled(rollingUpgradeFromRequest);
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        when(upgradeConverter.convert(captor.capture(), eq(true))).thenReturn(upgradeV4Request);
        when(distroXUpgradeService.upgradeCluster(upgradeV4Request, NameOrCrn.ofCrn(DATAHUB_CRN), false, WORKSPACE_ID)).thenReturn(distroXUpgradeV1Response);

        DistroXUpgradeV1Response result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.upgradeClusterByCrnInternal(DATAHUB_CRN, distroxUpgradeRequest, USER_CRN, rollingUpgradeParam));

        assertEquals(expectedRollingUpgradeParam, captor.getValue().getRollingUpgradeEnabled());
        assertEquals(distroXUpgradeV1Response, result);
    }

    @Test
    void testCcmUpgrade() {
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

    @Test
    void testOsUpgradeByUpgradeSetsInternal() {
        OrderedOSUpgradeSetRequest orderedOSUpgradeSetRequest = new OrderedOSUpgradeSetRequest();
        orderedOSUpgradeSetRequest.setImageId("imageId");
        List<OrderedOSUpgradeSet> upgradeSets = mock();
        orderedOSUpgradeSetRequest.setOrderedOsUpgradeSets(upgradeSets);
        FlowIdentifier expected = new FlowIdentifier(FlowType.FLOW, "1");
        when(distroXUpgradeService.triggerOsUpgradeByUpgradeSets(NameOrCrn.ofCrn(DATAHUB_CRN), WORKSPACE_ID, "imageId", upgradeSets)).thenReturn(expected);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.osUpgradeByUpgradeSetsInternal(DATAHUB_CRN, orderedOSUpgradeSetRequest));

        assertEquals(expected, result);
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrns() {
        List<String> datahubCrns = List.of(DATAHUB_CRN, "crn:cdp:datahub:us-west-1:1234:cluster:2");
        List<DistroXDatabaseUpgradeStatus> expected = List.of(
                DistroXDatabaseUpgradeStatus.upgradeRequired(datahubCrns.get(0), "14", "11"),
                DistroXDatabaseUpgradeStatus.upgradeNotRequired(datahubCrns.get(1), "14"));
        when(rdsUpgradeStatusService.getUpgradeRequiredByDatahubCrns(datahubCrns)).thenReturn(expected);

        List<DistroXDatabaseUpgradeStatus> result = underTest.getDatabaseServerUpgradeRequiredByDatahubCrns(datahubCrns);

        verify(rdsUpgradeStatusService).getUpgradeRequiredByDatahubCrns(datahubCrns);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrnsWhenNull() {
        when(rdsUpgradeStatusService.getUpgradeRequiredByDatahubCrns(null)).thenThrow(new BadRequestException("Datahub CRN list must not be null."));

        assertThrows(BadRequestException.class, () -> underTest.getDatabaseServerUpgradeRequiredByDatahubCrns(null));

        verify(rdsUpgradeStatusService).getUpgradeRequiredByDatahubCrns(null);
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrnsWhenEmpty() {
        List<String> datahubCrns = List.of();
        when(rdsUpgradeStatusService.getUpgradeRequiredByDatahubCrns(datahubCrns)).thenReturn(List.of());

        List<DistroXDatabaseUpgradeStatus> result = underTest.getDatabaseServerUpgradeRequiredByDatahubCrns(datahubCrns);

        verify(rdsUpgradeStatusService).getUpgradeRequiredByDatahubCrns(datahubCrns);
        assertThat(result).isEmpty();
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrnsWhenExceedsMaxBatchSize() {
        List<String> datahubCrns = Stream.generate(() -> DATAHUB_CRN).limit(51).toList();
        when(rdsUpgradeStatusService.getUpgradeRequiredByDatahubCrns(datahubCrns))
                .thenThrow(new BadRequestException("Datahub CRN list must not exceed 50 entries."));

        assertThrows(BadRequestException.class, () -> underTest.getDatabaseServerUpgradeRequiredByDatahubCrns(datahubCrns));

        verify(rdsUpgradeStatusService).getUpgradeRequiredByDatahubCrns(datahubCrns);
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrn() {
        DistroXDatabaseUpgradeStatus expected = DistroXDatabaseUpgradeStatus.upgradeRequired(DATAHUB_CRN, "14", "11");
        when(rdsUpgradeStatusService.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN))).thenReturn(expected);

        DistroXDatabaseUpgradeStatus result = underTest.getDatabaseServerUpgradeRequiredByDatahubCrn(DATAHUB_CRN);

        verify(rdsUpgradeStatusService).getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));
        assertEquals(expected, result);
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubName() {
        DistroXDatabaseUpgradeStatus expected = DistroXDatabaseUpgradeStatus.upgradeNotRequired(DATAHUB_CRN, "11");
        when(rdsUpgradeStatusService.getUpgradeRequired(NameOrCrn.ofName(CLUSTER_NAME))).thenReturn(expected);

        DistroXDatabaseUpgradeStatus result = underTest.getDatabaseServerUpgradeRequiredByDatahubName(CLUSTER_NAME);

        verify(rdsUpgradeStatusService).getUpgradeRequired(NameOrCrn.ofName(CLUSTER_NAME));
        assertEquals(expected, result);
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrnPropagatesFullContract() {
        DistroXDatabaseUpgradeStatus expected = DistroXDatabaseUpgradeStatus.upgradeRequired(DATAHUB_CRN, "14", "11");
        expected.setEolDate("2023-11-09");
        when(rdsUpgradeStatusService.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN))).thenReturn(expected);

        DistroXDatabaseUpgradeStatus result = underTest.getDatabaseServerUpgradeRequiredByDatahubCrn(DATAHUB_CRN);

        assertThat(result.getDatahubCrn()).isEqualTo(DATAHUB_CRN);
        assertThat(result.getUpgradeStatus()).isEqualTo("UPGRADE_REQUIRED");
        assertThat(result.getTargetMajorVersion()).isEqualTo("14");
        assertThat(result.getCurrentMajorVersion()).isEqualTo("11");
        assertThat(result.getEolDate()).isEqualTo("2023-11-09");
    }

    @Test
    void testGetDatabaseServerUpgradeRequiredByDatahubCrnPropagatesUnknownStatus() {
        DistroXDatabaseUpgradeStatus expected = DistroXDatabaseUpgradeStatus.unknown(DATAHUB_CRN);
        when(rdsUpgradeStatusService.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN))).thenReturn(expected);

        DistroXDatabaseUpgradeStatus result = underTest.getDatabaseServerUpgradeRequiredByDatahubCrn(DATAHUB_CRN);

        assertThat(result.getUpgradeStatus()).isEqualTo("UNKNOWN");
    }
}
