package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;

@ExtendWith(MockitoExtension.class)
public class UmsVirtualGroupCreateServiceTest {

    private static final String ENV_CRN1 = "env crn 1";

    private static final String ENV_CRN2 = "env crn 2";

    private static final Set<String> ENV_CRNS = Set.of(ENV_CRN1, ENV_CRN2);

    private static final String ACCOUNT_ID = "account id";

    @Mock
    private VirtualGroupService virtualGroupService;

    @InjectMocks
    private UmsVirtualGroupCreateService victim;

    @Test
    public void testCreateVirtualGroupsForEachEnvironments() {

        victim.createVirtualGroups(ACCOUNT_ID, ENV_CRNS);

        verify(virtualGroupService).createVirtualGroups(ACCOUNT_ID, ENV_CRN1);
        verify(virtualGroupService).createVirtualGroups(ACCOUNT_ID, ENV_CRN2);
    }

    @Test
    public void testFailSafeVirtualGroupCreation() {
        when(virtualGroupService.createVirtualGroups(ACCOUNT_ID, ENV_CRN1)).thenThrow(RuntimeException.class);

        victim.createVirtualGroups(ACCOUNT_ID, ENV_CRNS);

        verify(virtualGroupService).createVirtualGroups(ACCOUNT_ID, ENV_CRN2);
    }
}