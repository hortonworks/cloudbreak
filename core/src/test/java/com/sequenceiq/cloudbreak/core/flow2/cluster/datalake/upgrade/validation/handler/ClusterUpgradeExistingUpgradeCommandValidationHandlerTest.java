package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeExistingUpgradeCommandValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeExistingUpgradeCommandValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "aStack";

    private static final String STACK_VERSION = "7.2.7";

    private static final String CDH = "CDH";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stack;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterApi connector;

    @Mock
    private ClusterStatusService clusterStatusService;

    @InjectMocks
    private ClusterUpgradeExistingUpgradeCommandValidationHandler underTest;

    @BeforeEach
    public void setup() {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        lenient().when(stack.getStack()).thenReturn(stackView);
        lenient().when(stackView.getName()).thenReturn(STACK_NAME);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
    }

    @Test
    void testUpgradeCommandDoesNotExistThenValidationShouldPass() {

        when(clusterStatusService.findCommand(stack, ClusterCommandType.UPGRADE_CLUSTER)).thenReturn(Optional.empty());

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
    }

    @Test
    void testUpgradeCommandNotActiveNotSuccessfulNotRetryableThenValidationShouldPass() {

        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setActive(false);
        command.setSuccess(false);
        command.setRetryable(false);

        when(clusterStatusService.findCommand(stack, ClusterCommandType.UPGRADE_CLUSTER)).thenReturn(Optional.of(command));

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
    }

    @Test
    void testUpgradeCommandNotActiveSuccessfulNotRetryableThenValidationShouldPass() {

        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setActive(false);
        command.setSuccess(true);
        command.setRetryable(false);

        when(clusterStatusService.findCommand(stack, ClusterCommandType.UPGRADE_CLUSTER)).thenReturn(Optional.of(command));

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
    }

    @Test
    void testUpgradeCommandNotActiveNotSuccessfulRetryableAndEmptyActiveRuntimeParcelVersionThenValidationShouldPass() {

        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setActive(false);
        command.setSuccess(false);
        command.setRetryable(true);

        when(clusterStatusService.findCommand(stack, ClusterCommandType.UPGRADE_CLUSTER)).thenReturn(Optional.of(command));
        when(connector.gatherInstalledParcels(STACK_NAME)).thenReturn(Collections.emptySet());

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
    }

    @Test
    void testUpgradeCommandNotActiveNotSuccessfulRetryableAndActiveRuntimeParcelVersionAndTargetBuildMatchThenValidationShouldPass() {

        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setActive(false);
        command.setSuccess(false);
        command.setRetryable(true);

        when(clusterStatusService.findCommand(stack, ClusterCommandType.UPGRADE_CLUSTER)).thenReturn(Optional.of(command));
        ParcelInfo activeParcel = new ParcelInfo(CDH, "7.2.7-1.cdh7.2.7.p7.12569826", ParcelStatus.ACTIVATED);
        when(connector.gatherInstalledParcels(STACK_NAME)).thenReturn(Collections.singleton(activeParcel));

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent("12569826"));

        assertEquals(FINISH_CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
    }

    @Test
    void testUpgradeCommandNotActiveNotSuccessfulRetryableAndActiveRuntimeParcelVersionAndTargetBuildNotMatchThenValidationShouldFail() {

        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setActive(false);
        command.setSuccess(false);
        command.setRetryable(true);

        when(clusterStatusService.findCommand(stack, ClusterCommandType.UPGRADE_CLUSTER)).thenReturn(Optional.of(command));
        ParcelInfo activeParcel = new ParcelInfo(CDH, "7.2.7-1.cdh7.2.7.p7.12569826", ParcelStatus.ACTIVATED);
        when(connector.gatherInstalledParcels(STACK_NAME)).thenReturn(Collections.singleton(activeParcel));

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent("23569826"));

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.selector(), nextFlowStepSelector.selector());
        String expectedMessage = "Existing upgrade command found for active runtime 7.2.7-1.cdh7.2.7.p7.12569826, upgrading to a different runtime version "
                + "(7.2.7-23569826) is not allowed! Possible solutions: "
                + "#1, retry the upgrade with the same target runtime. "
                + "#2, complete the upgrade manually in Cloudera Manager. "
                + "#3, recover the cluster";
        assertEquals(expectedMessage, ((ClusterUpgradeValidationFailureEvent) nextFlowStepSelector).getException().getMessage());
    }

    private HandlerEvent<ClusterUpgradeExistingUpgradeCommandValidationEvent> getHandlerEvent() {
        return getHandlerEvent("");
    }

    private HandlerEvent<ClusterUpgradeExistingUpgradeCommandValidationEvent> getHandlerEvent(String buildNumber) {
        Image targetImage = mock(Image.class);
        Map<String, String> packageVersions = new java.util.HashMap<>();
        packageVersions.put(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), buildNumber);
        packageVersions.put(ImagePackageVersion.STACK.getKey(), STACK_VERSION);

        lenient().when(targetImage.getPackageVersions()).thenReturn(packageVersions);
        ClusterUpgradeExistingUpgradeCommandValidationEvent clusterUpgradeImageValidationEvent =
                new ClusterUpgradeExistingUpgradeCommandValidationEvent(1L, targetImage);
        HandlerEvent<ClusterUpgradeExistingUpgradeCommandValidationEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(clusterUpgradeImageValidationEvent);
        return handlerEvent;
    }
}