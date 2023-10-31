package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

@ExtendWith(MockitoExtension.class)
public class UmsVirtualGroupCreateServiceTest {

    private static final String ENV_CRN1 = "env crn 1";

    private static final String ENV_CRN2 = "env crn 2";

    private static final String ACCOUNT_ID = "account id";

    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private StackUserSyncView stack1;

    @Mock
    private StackUserSyncView stack2;

    @Mock
    private StackUserSyncView unavailableStack;

    private List<StackUserSyncView> stacks;

    @InjectMocks
    private UmsVirtualGroupCreateService underTest;

    @BeforeEach
    public void initTests() {
        when(stack1.environmentCrn()).thenReturn(ENV_CRN1);
        when(stack1.isAvailable()).thenReturn(true);
        when(stack2.environmentCrn()).thenReturn(ENV_CRN2);
        when(stack2.isAvailable()).thenReturn(true);
        when(unavailableStack.isAvailable()).thenReturn(false);

        //Stacks are added multiple times to verify createVirtualGroups method is called 1 time per environment crn.
        stacks = List.of(stack1, stack2, stack1, stack2, unavailableStack);
    }

    @Test
    public void testCreateVirtualGroupsForEachStacks() {
        underTest.createVirtualGroups(ACCOUNT_ID, stacks);

        verify(virtualGroupService).createVirtualGroups(ACCOUNT_ID, ENV_CRN1);
        verify(virtualGroupService).createVirtualGroups(ACCOUNT_ID, ENV_CRN2);
    }

    @Test
    public void testFailSafeVirtualGroupCreation() {
        when(virtualGroupService.createVirtualGroups(ACCOUNT_ID, ENV_CRN1)).thenThrow(RuntimeException.class);

        underTest.createVirtualGroups(ACCOUNT_ID, stacks);

        verify(virtualGroupService).createVirtualGroups(ACCOUNT_ID, ENV_CRN2);
    }
}