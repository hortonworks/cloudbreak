package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.SudoCommand;
import com.sequenceiq.freeipa.client.model.SudoRule;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

@ExtendWith(MockitoExtension.class)
public class SudoRuleServiceTest {

    private static final String RULE_NAME = "rule-name";

    private static final String ALLOW_COMMAND1 = "allowcommand1";

    private static final String ALLOW_COMMAND2 = "allowcommand2";

    private static final String DENY_COMMAND1 = "denycommand1";

    private static final String DENY_COMMAND2 = "denycommand2";

    private static final String GROUP = "userGroup";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private StackUserSyncView stack;

    @InjectMocks
    private SudoRuleService victim;

    @BeforeEach
    public void initTests() {
        Field ruleName = ReflectionUtils.findField(SudoRuleService.class, "ruleName");
        ReflectionUtils.makeAccessible(ruleName);
        ReflectionUtils.setField(ruleName, victim, RULE_NAME);

        Field allowCommands = ReflectionUtils.findField(SudoRuleService.class, "allowCommands");
        ReflectionUtils.makeAccessible(allowCommands);
        ReflectionUtils.setField(allowCommands, victim, Set.of(ALLOW_COMMAND1, ALLOW_COMMAND2));

        Field denyCommands = ReflectionUtils.findField(SudoRuleService.class, "denyCommands");
        ReflectionUtils.makeAccessible(denyCommands);
        ReflectionUtils.setField(denyCommands, victim, Set.of(DENY_COMMAND1, DENY_COMMAND2));

        when(stack.isAvailable()).thenReturn(true);

        victim.postConstruct();
    }

    @Test
    public void shouldThrowFreeIpaClientExceptionInCaseOfMissinGroup() throws FreeIpaClientException {
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn(GROUP);
        when(freeIpaClient.groupShow(GROUP)).thenThrow(new FreeIpaClientException(null));

        assertThrows(FreeIpaClientException.class, () -> victim.setupSudoRule(stack, freeIpaClient));
    }

    @Test
    public void shouldThrowIllegalStateExceptionInCaseOfExistingSudoRuleButNotAllHostGroup() throws FreeIpaClientException {
        Optional<SudoRule> sudoRule = Optional.of(new SudoRule());

        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn(GROUP);
        when(freeIpaClient.groupShow(GROUP)).thenReturn(mock(Group.class));
        when(freeIpaClient.sudoRuleShow(RULE_NAME)).thenReturn(sudoRule);

        assertThrows(IllegalStateException.class, () -> victim.setupSudoRule(stack, freeIpaClient));
    }

    @Test
    public void shouldCreateValidSudoRuleCommandAndGroupAssignment() throws Exception {
        SudoRule sudoRule = aValidSudoRule();

        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn(GROUP);
        when(freeIpaClient.groupShow(GROUP)).thenReturn(mock(Group.class));
        when(freeIpaClient.sudoRuleShow(RULE_NAME)).thenReturn(Optional.empty());
        when(freeIpaClient.sudoRuleAdd(RULE_NAME, true)).thenReturn(sudoRule);
        when(freeIpaClient.sudoCommandFindAll()).thenReturn(Set.of());

        victim.setupSudoRule(stack, freeIpaClient);

        verify(freeIpaClient).sudoCommandAdd(ALLOW_COMMAND1);
        verify(freeIpaClient).sudoCommandAdd(ALLOW_COMMAND2);
        verify(freeIpaClient).sudoCommandAdd(DENY_COMMAND1);
        verify(freeIpaClient).sudoCommandAdd(DENY_COMMAND2);
        verify(freeIpaClient).sudoRuleAddAllowCommand(RULE_NAME, ALLOW_COMMAND1);
        verify(freeIpaClient).sudoRuleAddAllowCommand(RULE_NAME, ALLOW_COMMAND2);
        verify(freeIpaClient).sudoRuleAddDenyCommand(RULE_NAME, DENY_COMMAND1);
        verify(freeIpaClient).sudoRuleAddDenyCommand(RULE_NAME, DENY_COMMAND2);
        verify(freeIpaClient).sudoRuleAddGroup(RULE_NAME, GROUP);
    }

    @Test
    public void shouldAddCommandsAndGroup() throws Exception {
        Optional<SudoRule> sudoRule = Optional.of(aValidSudoRule());

        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn(GROUP);
        when(freeIpaClient.groupShow(GROUP)).thenReturn(mock(Group.class));
        when(freeIpaClient.sudoRuleShow(RULE_NAME)).thenReturn(sudoRule);
        when(freeIpaClient.sudoCommandFindAll()).thenReturn(Set.of());

        victim.setupSudoRule(stack, freeIpaClient);

        verify(freeIpaClient).sudoCommandAdd(ALLOW_COMMAND1);
        verify(freeIpaClient).sudoCommandAdd(ALLOW_COMMAND2);
        verify(freeIpaClient).sudoCommandAdd(DENY_COMMAND1);
        verify(freeIpaClient).sudoCommandAdd(DENY_COMMAND2);
        verify(freeIpaClient).sudoRuleAddAllowCommand(RULE_NAME, ALLOW_COMMAND1);
        verify(freeIpaClient).sudoRuleAddAllowCommand(RULE_NAME, ALLOW_COMMAND2);
        verify(freeIpaClient).sudoRuleAddDenyCommand(RULE_NAME, DENY_COMMAND1);
        verify(freeIpaClient).sudoRuleAddDenyCommand(RULE_NAME, DENY_COMMAND2);
        verify(freeIpaClient).sudoRuleAddGroup(RULE_NAME, GROUP);
    }

    @Test
    public void shouldNotAddExistingCommands() throws Exception {
        Optional<SudoRule> sudoRule = Optional.of(aValidSudoRule());
        sudoRule.get().setAllowSudoCommands(List.of(ALLOW_COMMAND1));
        sudoRule.get().setDenySudoCommands(List.of(DENY_COMMAND1));

        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn(GROUP);
        when(freeIpaClient.groupShow(GROUP)).thenReturn(mock(Group.class));
        when(freeIpaClient.sudoRuleShow(RULE_NAME)).thenReturn(sudoRule);
        when(freeIpaClient.sudoCommandFindAll()).thenReturn(Set.of(aSudoCommand(ALLOW_COMMAND1), aSudoCommand(DENY_COMMAND1)));

        victim.setupSudoRule(stack, freeIpaClient);

        verify(freeIpaClient).sudoCommandAdd(ALLOW_COMMAND2);
        verify(freeIpaClient).sudoCommandAdd(DENY_COMMAND2);
        verify(freeIpaClient).sudoRuleAddAllowCommand(RULE_NAME, ALLOW_COMMAND2);
        verify(freeIpaClient).sudoRuleAddDenyCommand(RULE_NAME, DENY_COMMAND2);
        verify(freeIpaClient).sudoRuleAddGroup(RULE_NAME, GROUP);
        verifyNoMoreInteractions(freeIpaClient);
    }

    @Test
    public void shouldNotChangeRuleInCaseOfEverythingInPlace() throws Exception {
        Optional<SudoRule> sudoRule = Optional.of(aValidSudoRule());
        sudoRule.get().setAllowSudoCommands(List.of(ALLOW_COMMAND1, ALLOW_COMMAND2));
        sudoRule.get().setDenySudoCommands(List.of(DENY_COMMAND1, DENY_COMMAND2));
        sudoRule.get().setUserGroups(List.of(GROUP));

        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn(GROUP);
        when(freeIpaClient.groupShow(GROUP)).thenReturn(mock(Group.class));
        when(freeIpaClient.sudoRuleShow(RULE_NAME)).thenReturn(sudoRule);
        when(freeIpaClient.sudoCommandFindAll()).thenReturn(Set.of(
                aSudoCommand(ALLOW_COMMAND1),
                aSudoCommand(ALLOW_COMMAND2),
                aSudoCommand(DENY_COMMAND1),
                aSudoCommand(DENY_COMMAND2)));

        victim.setupSudoRule(stack, freeIpaClient);

        verifyNoMoreInteractions(freeIpaClient);
    }

    @Test
    public void shouldNotPerformSudoSetupInCaseOfMissingVirtualGroup() throws Exception {
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(virtualGroupService.getVirtualGroup(any(), eq(UmsVirtualGroupRight.ALLOW_PRIVILEGED_OS_OPERATIONS))).thenReturn("");

        victim.setupSudoRule(stack, freeIpaClient);

        verifyNoMoreInteractions(freeIpaClient);
    }

    @Test
    public void shouldNotPerformSudoSetupInCaseNotAvailableStack() throws Exception {
        when(stack.isAvailable()).thenReturn(false);

        victim.setupSudoRule(stack, freeIpaClient);

        verifyNoMoreInteractions(freeIpaClient);
    }

    private SudoRule aValidSudoRule() {
        SudoRule sudoRule = new SudoRule();
        sudoRule.setHostCategory("all");

        return sudoRule;
    }

    private SudoCommand aSudoCommand(String command) {
        SudoCommand sudoCommand = new SudoCommand();
        sudoCommand.setSudocmd(command);

        return sudoCommand;
    }
}