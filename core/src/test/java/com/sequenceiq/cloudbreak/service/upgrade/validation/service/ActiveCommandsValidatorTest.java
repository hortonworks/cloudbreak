package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@ExtendWith(MockitoExtension.class)
public class ActiveCommandsValidatorTest {

    private static final String START_TIME = "start-time";

    private static final String INTERRUPTABLE_COMMAND_1 = "global-estimate-host-statistics";

    private static final String INTERRUPTABLE_COMMAND_2 = "ProcessStalenessCheckCommand";

    private static final String NON_INTERRUPTABLE_COMMAND_1 = "non-interruptable-1";

    private static final String NON_INTERRUPTABLE_COMMAND_2 = "non-interruptable-2";

    @InjectMocks
    private ActiveCommandsValidator underTest;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi connector;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private StackDto stack;

    private ServiceUpgradeValidationRequest request;

    @BeforeEach
    void before() {
        request = new ServiceUpgradeValidationRequest(stack, true, true, null, false);
    }

    @Test
    public void testValidateIfActiveCommandsListIsEmpty() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        // WHEN
        underTest.validate(request);
        // THEN no exception is thrown
    }

    @Test
    public void testValidateIfActiveCommandsListIsNull() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getActiveCommandsList()).thenReturn(null);
        // WHEN
        underTest.validate(request);
        // THEN no exception is thrown
    }

    @Test
    public void testValidateIfOnlyInterruptableActiveCommandsArePresent() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        initGlobalPrivateFields();
        when(clusterStatusService.getActiveCommandsList()).thenReturn(List.of(createCommand(INTERRUPTABLE_COMMAND_1), createCommand(INTERRUPTABLE_COMMAND_2)));
        // WHEN
        underTest.validate(request);
        // THEN no exception is thrown
    }

    @Test
    public void testValidateIfOnlyNonInterruptableActiveCommandsArePresent() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        initGlobalPrivateFields();
        when(clusterStatusService.getActiveCommandsList()).thenReturn(
                List.of(createCommand(NON_INTERRUPTABLE_COMMAND_1), createCommand(NON_INTERRUPTABLE_COMMAND_2)));
        // WHEN
        UpgradeValidationFailedException ex = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(request));
        // THEN exception is thrown
        assertEquals("There are active commands running on CM that are not interruptable, upgrade is not possible. " +
                "Active commands: [ClusterManagerCommand{id=1, name='non-interruptable-1', startTime='start-time', endTime='null', active=null, " +
                "success=null, resultMessage='null', retryable=null}, ClusterManagerCommand{id=1, name='non-interruptable-2', startTime='start-time', " +
                "endTime='null', active=null, success=null, resultMessage='null', retryable=null}]", ex.getMessage());
    }

    @Test
    public void testValidateIfOnlyBothNonInterruptableAndInterruptableActiveCommandsArePresent() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        initGlobalPrivateFields();
        when(clusterStatusService.getActiveCommandsList()).thenReturn(
                List.of(createCommand(INTERRUPTABLE_COMMAND_1), createCommand(NON_INTERRUPTABLE_COMMAND_1)));
        // WHEN
        UpgradeValidationFailedException ex = assertThrows(UpgradeValidationFailedException.class,
                () -> underTest.validate(request));
        // THEN exception is thrown
        assertEquals("There are active commands running on CM that are not interruptable, upgrade is not possible. " +
                "Active commands: [ClusterManagerCommand{id=1, name='non-interruptable-1', startTime='start-time', endTime='null', active=null, " +
                "success=null, resultMessage='null', retryable=null}]", ex.getMessage());
    }

    private void initGlobalPrivateFields() {
        Field interruptableCommands = ReflectionUtils.findField(ActiveCommandsValidator.class, "interruptableCommands");
        ReflectionUtils.makeAccessible(interruptableCommands);
        ReflectionUtils.setField(interruptableCommands, underTest, Set.of(INTERRUPTABLE_COMMAND_1, INTERRUPTABLE_COMMAND_2));
    }

    private ClusterManagerCommand createCommand(String name) {
        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setId(BigDecimal.ONE);
        command.setStartTime(START_TIME);
        command.setName(name);
        return command;
    }
}
